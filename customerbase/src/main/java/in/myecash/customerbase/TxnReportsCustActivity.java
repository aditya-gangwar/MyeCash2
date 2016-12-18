package in.myecash.customerbase;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import in.myecash.appbase.DatePickerDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.TxnReportsHelper;
import in.myecash.common.CommonUtils;
import in.myecash.common.CsvConverter;
import in.myecash.common.DateUtil;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 04-04-2016.
 */
public class TxnReportsCustActivity extends AppCompatActivity implements
        View.OnClickListener, MyRetainedFragment.RetainedFragmentIf,
        DatePickerDialog.DatePickerIf, TxnListFragment.TxnListFragmentIf,
        DialogFragmentWrapper.DialogFragmentWrapperIf, TxnDetailsDialog.TxnDetailsDialogIf,
        TxnReportsHelper.TxnReportsHelperIf {
    private static final String TAG = "TxnReportsActivity";

    public static final String EXTRA_MERCHANT_ID = "extraMchntId";
    public static final String EXTRA_MERCHANT_NAME = "extraMchntName";

    private static final String RETAINED_FRAGMENT = "retainedFragReports";
    private static final String DIALOG_DATE_FROM = "DialogDateFrom";
    private static final String DIALOG_DATE_TO = "DialogDateTo";
    private static final String TXN_LIST_FRAGMENT = "TxnListFragment";

    // All required date formatters
    private SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    FragmentManager mFragMgr;
    MyRetainedFragment mWorkFragment;

    private Date mNow;
    private Date mTodayEoD;
    private String mMerchantId;
    private String mMerchantName;

    // Store and restore as part of instance state
    private TxnReportsHelper mHelper;
    private Date mFromDate;
    private Date mToDate;
    private int mDetailedTxnPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txn_report);

        // gets handlers to screen resources
        bindUiResources();

        // Initialize retained fragment before other fragments
        // Check to see if we have retained the worker fragment.
        mFragMgr = getFragmentManager();
        mWorkFragment = (MyRetainedFragment)mFragMgr.findFragmentByTag(RETAINED_FRAGMENT);
        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            LogMy.d(TAG, "Creating retained fragment instance");
            mWorkFragment = new MyRetainedFragment();
            mFragMgr.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT).commit();
        }

        // Init date members
        mNow = new Date();
        // end of today
        DateUtil now = new DateUtil(mNow, TimeZone.getDefault());
        mTodayEoD = now.toEndOfDay().getTime();
        LogMy.d( TAG, "mNow: "+String.valueOf(mNow.getTime()) +", mTodayEoD: "+ String.valueOf(mTodayEoD.getTime()) );

        // create helper instance
        if(savedInstanceState==null) {
            mHelper = new TxnReportsHelper(this);
        } else {
            mHelper = mWorkFragment.mTxnReportHelper;
        }

        initToolbar();
        initDateInputs(savedInstanceState);

        mBtnGetReport.setOnClickListener(this);
        if(savedInstanceState!=null) {
            mDetailedTxnPos = savedInstanceState.getInt("mDetailedTxnPos");
        } else {
            mDetailedTxnPos = -1;
        }

        // get passed merchant details
        mMerchantId = getIntent().getStringExtra(EXTRA_MERCHANT_ID);
        mMerchantName = getIntent().getStringExtra(EXTRA_MERCHANT_NAME);
    }

    @Override
    public void onBgThreadCreated() {
        // was facing race condition - where mBtnGetReport.performClick() was getting called
        // before bg thread is initialized properly
        // so added this callback

        // if 'Merchant ID' not provided - means fetch only latest txns from DB table
        if(mMerchantId==null || mMerchantId.isEmpty()) {
            mFromDate = new Date();
            mFromDate.setTime(mHelper.getTxnInDbFrom().getTime());
            mToDate = mTodayEoD;
            // simulate click to generate report
            mBtnGetReport.performClick();
        } else {
            mInputMerchant.setText(mMerchantName);
        }
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        try {
            if (vId == R.id.input_date_from) {
                // Find the minimum date for DatePicker
                DateUtil minFrom = new DateUtil(new Date(), TimeZone.getDefault());
                minFrom.removeDays(MyGlobalSettings.getCustTxnHistoryDays());

                DialogFragment fromDialog = DatePickerDialog.newInstance(mFromDate, minFrom.getTime(), mNow);
                fromDialog.show(getFragmentManager(), DIALOG_DATE_FROM);

            } else if (vId == R.id.input_date_to) {
                if (mFromDate == null) {
                    AppCommonUtil.toast(this, "Set From Date");
                } else {
                    DialogFragment toDialog = DatePickerDialog.newInstance(mToDate, mFromDate, mNow);
                    toDialog.show(getFragmentManager(), DIALOG_DATE_TO);
                }

            } else if (vId == R.id.btn_get_report) {
                // clear old data
                //mWorkFragment.mAllFiles.clear();
                //mWorkFragment.mMissingFiles.clear();
                //mWorkFragment.mTxnsFromCsv.clear();
                if (mWorkFragment.mLastFetchTransactions != null) {
                    mWorkFragment.mLastFetchTransactions.clear();
                    mWorkFragment.mLastFetchTransactions = null;
                }
                mHelper.startTxnFetch(mFromDate, mToDate, mMerchantId, CustomerUser.getInstance().getCustomer().getPrivate_id());

            } else {
            }
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is ReportsActivity:onClick: "+vId, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            //mWorkFragment.mTxnsFromCsv.clear();
        }
    }

    private void initDateInputs(Bundle instanceState) {
        if(instanceState==null) {
            // mFromDate as 'start of today' i.e. todayMidnight
            DateUtil now = new DateUtil(mNow, TimeZone.getDefault());
            mFromDate = now.toMidnight().getTime();
            mToDate = mTodayEoD;
        } else {
            mFromDate = (Date)instanceState.getSerializable("mFromDate");
            mToDate = (Date)instanceState.getSerializable("mToDate");
        }
        mInputDateFrom.setText(mSdfOnlyDateDisplay.format(mFromDate));
        mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));

        mInputDateFrom.setOnClickListener(this);
        mInputDateTo.setOnClickListener(this);
    }

    private void initToolbar() {
        LogMy.d(TAG, "In initToolbar");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_report);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
    }

    @Override
    public void onDateSelected(Date argDate, String tag) {
        LogMy.d(TAG, "Selected date: " + argDate.toString());
        if(tag.equals(DIALOG_DATE_FROM)) {
            mFromDate = argDate;
            mInputDateFrom.setText(mSdfOnlyDateDisplay.format(mFromDate));
        } else {
            // Increment by 1 day, and then take midnight
            DateUtil to = new DateUtil(argDate, TimeZone.getDefault());
            to.toEndOfDay();
            mToDate = to.getTime();
            mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));
        }
    }

    @Override
    public void fetchTxnsFromDB(String whereClause) {
        // show progress dialog
        AppCommonUtil.showProgressDialog(this, AppConstants.progressReports);
        mWorkFragment.fetchTransactions(whereClause);
    }

    @Override
    public void fetchTxnFiles(List<String> missingFiles) {
        mWorkFragment.mMissingFiles = missingFiles;
        // show progress dialog
        AppCommonUtil.showProgressDialog(this, AppConstants.progressReports);
        mWorkFragment.fetchTxnFiles(this);
    }

    @Override
    public void onFinalTxnSetAvailable(List<Transaction> allTxns) {
        mWorkFragment.mLastFetchTransactions = allTxns;

        if(allTxns==null || allTxns.isEmpty()) {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.NO_DATA_FOUND), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            startTxnListFragment();
        }
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        AppCommonUtil.cancelProgressDialog(true);

        try {
            switch(operation) {
                case MyRetainedFragment.REQUEST_FETCH_TXNS:
                    if (errorCode == ErrorCodes.NO_ERROR ||
                            errorCode == ErrorCodes.NO_DATA_FOUND) {
                        mHelper.onDbTxnsAvailable(mWorkFragment.mLastFetchTransactions);
                    } else {
                        DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    }
                    break;

                case MyRetainedFragment.REQUEST_FETCH_TXN_FILES:
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        // all files should now be available locally
                        mHelper.onAllTxnFilesAvailable(false);
                    } else if (errorCode == ErrorCodes.FILE_NOT_FOUND) {
                        // one or more files not found, may be corresponding day txns are present in table, try to fetch the same
                        mHelper.onAllTxnFilesAvailable(true);
                    } else {
                        DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    }
                    break;
            }

        } catch (Exception e) {
            LogMy.e(TAG, "Exception is ReportsActivity:onBgProcessResponse: "+operation+": "+errorCode, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    private void startTxnListFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(TXN_LIST_FRAGMENT);
        if (fragment == null) {
            LogMy.d(TAG,"Creating new txn list fragment");

            // Create new fragment and transaction
            //fragment = new TxnListFragment_2();
            fragment = TxnListFragment.getInstance(mFromDate,mToDate,mMerchantName);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Add over the existing fragment
            mMainLayout.setVisibility(View.GONE);
            mFragmentContainer.setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragment_container_report, fragment, TXN_LIST_FRAGMENT);
            transaction.addToBackStack(TXN_LIST_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public void setToolbarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public MyRetainedFragment getRetainedFragment() {
        return mWorkFragment;
    }

    @Override
    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        // do nothing
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        LogMy.d(TAG, "In onBackPressed: " + count);

        if (count == 0) {
            super.onBackPressed();
        } else {
            if(!mWorkFragment.mInPauseState) {
                getFragmentManager().popBackStackImmediate();
            }

            if(mMerchantId==null || mMerchantId.isEmpty()) {
                // Case when latest txns are directly shown
                // i.e. the main layout to enter from, to dates - was not shown
                super.onBackPressed();

            } else if(mFragmentContainer.getVisibility()==View.VISIBLE && count==1) {
                getSupportActionBar().setTitle("Transactions");
                mMainLayout.setVisibility(View.VISIBLE);
                mFragmentContainer.setVisibility(View.GONE);
            }
        }

    }

    /**
     * react to the user tapping the back/up icon in the action bar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // this takes the user 'back', as if they pressed the left-facing triangle icon on the main android toolbar.
                // if this doesn't work as desired, another possibility is to call `finish()` here.
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private EditText mInputDateFrom;
    private EditText mInputDateTo;
    private EditText mInputMerchant;
    private AppCompatButton mBtnGetReport;

    private LinearLayout mMainLayout;
    private FrameLayout mFragmentContainer;

    private void bindUiResources() {
        mInputDateFrom = (EditText) findViewById(R.id.input_date_from);
        mInputDateTo = (EditText) findViewById(R.id.input_date_to);
        mInputMerchant = (EditText) findViewById(R.id.input_merchant);
        mBtnGetReport = (AppCompatButton) findViewById(R.id.btn_get_report);

        mMainLayout = (LinearLayout) findViewById(R.id.layout_report_main);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_report);
    }

    @Override
    protected void onResume() {
        LogMy.d(TAG, "In onResume: ");
        super.onResume();
        mWorkFragment.mInPauseState = false;
        if(AppCommonUtil.getProgressDialogMsg()!=null) {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
        }
    }

    @Override
    protected void onPause() {
        LogMy.d(TAG,"In onPause: ");
        super.onPause();
        mWorkFragment.mInPauseState = true;
        AppCommonUtil.cancelProgressDialog(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("mFromDate", mFromDate);
        outState.putSerializable("mToDate", mToDate);
        outState.putInt("mDetailedTxnPos", mDetailedTxnPos);
        mWorkFragment.mTxnReportHelper = mHelper;
    }

}
