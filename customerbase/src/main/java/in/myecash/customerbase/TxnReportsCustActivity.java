package in.myecash.customerbase;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import in.myecash.appbase.BaseActivity;
import in.myecash.appbase.MyDatePickerDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.TxnReportsHelper2;
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
public class TxnReportsCustActivity extends BaseActivity implements
        MyRetainedFragment.RetainedFragmentIf,
        MyDatePickerDialog.MyDatePickerIf, TxnListFragment.TxnListFragmentIf,
        DialogFragmentWrapper.DialogFragmentWrapperIf, TxnDetailsDialog.TxnDetailsDialogIf,
        TxnReportsHelper2.TxnReportsHelper2If {
    private static final String TAG = "CustApp-TxnReportsActivity";

    public static final String EXTRA_MERCHANT_ID = "extraMchntId";
    public static final String EXTRA_MERCHANT_NAME = "extraMchntName";

    private static final String RETAINED_FRAGMENT = "retainedFragReports";
    private static final String DIALOG_DATE_FROM = "DialogDateFrom";
    private static final String DIALOG_DATE_TO = "DialogDateTo";
    private static final String DIALOG_ERROR_NOTIFY = "DialogErrorNotify";

    private static final String TXN_LIST_FRAGMENT = "TxnListFragment";
    private static final String DIALOG_MERCHANT_DETAILS = "dialogMerchantDetails";

    // All required date formatters
    private SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    FragmentManager mFragMgr;
    MyRetainedFragment mWorkFragment;

    //private Date mNow;
    //private Date mTodayEoD;
    private String mMerchantId;
    private String mMerchantName;

    // Store and restore as part of instance state
    private TxnReportsHelper2 mHelper;
    private Date mFromDate;
    private Date mToDate;
    //private int mDetailedTxnPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txn_report_cust);
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

        // get passed merchant details
        mMerchantId = getIntent().getStringExtra(EXTRA_MERCHANT_ID);
        mMerchantName = getIntent().getStringExtra(EXTRA_MERCHANT_NAME);

        // create/restore helper instance
        if(savedInstanceState==null) {
            mHelper = new TxnReportsHelper2(this);
        } else {
            mHelper = mWorkFragment.mTxnReportHelper;
        }
        // init toolbar
        initToolbar();
        String txt = String.format(getString(R.string.mchnt_txn_history_info), MyGlobalSettings.getMchntTxnHistoryDays().toString() );
        mLabelInfo.setText(txt);

        // Init date members
        // end of today
        //mNow = new Date();
        //DateUtil now = new DateUtil(mNow, TimeZone.getDefault());
        //mTodayEoD = now.toEndOfDay().getTime();
        //LogMy.d( TAG, "mNow: "+String.valueOf(mNow.getTime()) +", mTodayEoD: "+ String.valueOf(mTodayEoD.getTime()) );

        if(mMerchantId==null || mMerchantId.isEmpty()) {
            mMerchantLayout.setVisibility(View.GONE);
        } else {
            mMerchantLayout.setVisibility(View.VISIBLE);
            mInputMerchant.setText(mMerchantName);
        }

        initDateInputs(savedInstanceState);
        mBtnGetReport.setOnClickListener(this);

        /*if(savedInstanceState!=null) {
            mDetailedTxnPos = savedInstanceState.getInt("mDetailedTxnPos");
        } else {
            mDetailedTxnPos = -1;
        }*/
    }

    @Override
    public void onBgThreadCreated() {
        // was facing race condition - where mBtnGetReport.performClick() was getting called
        // before bg thread is initialized properly
        // so added this callback

        // if 'Merchant ID' not provided - means fetch only latest txns from DB table
        /*if(mMerchantId==null || mMerchantId.isEmpty()) {
            mFromDate = new Date();
            mFromDate.setTime(mHelper.getTxnInDbFrom().getTime());
            mToDate = new Date();
            mInputMerchant.setText("ALL");
            // simulate click to generate report
            mBtnGetReport.performClick();
        } else {
            mInputMerchant.setText(mMerchantName);
        }*/
    }

    @Override
    public boolean handleTouchUp(View v) {
        try {
            if(!mWorkFragment.getResumeOk()) {
                return true;
            }
            int vId = v.getId();
            LogMy.d(TAG, "In onTouch: " + vId);

            if (vId == R.id.input_date_from) {
                // Find the minimum date for DatePicker
                DateUtil minFrom = new DateUtil(new Date(), TimeZone.getDefault());
                minFrom.removeDays(MyGlobalSettings.getCustTxnHistoryDays());

                Date now = new Date();
                Date maxTo;
                if(mToDate==null) {
                    maxTo = now;
                } else {
                    maxTo = new Date(Math.min(mToDate.getTime(), now.getTime()));
                }

                DialogFragment fromDialog = MyDatePickerDialog.newInstance(mFromDate.getTime(), minFrom.getTime().getTime(), maxTo.getTime());
                fromDialog.show(getFragmentManager(), DIALOG_DATE_FROM);

            } else if (vId == R.id.input_date_to) {
                if (mFromDate == null) {
                    AppCommonUtil.toast(this, "Set From Date");
                } else {
                    DialogFragment toDialog = MyDatePickerDialog.newInstance(mToDate.getTime(), mFromDate.getTime(), System.currentTimeMillis());
                    toDialog.show(getFragmentManager(), DIALOG_DATE_TO);
                }

            }
        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception in TxnReportsCustActivity", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DIALOG_ERROR_NOTIFY);
        }
        return true;
    }

    @Override
    public void handleBtnClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);
        if (vId == R.id.btn_get_report) {
            startTxnFetch();
        }
    }

    /*@Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        try {
            if(!mWorkFragment.getResumeOk()) {
                return true;
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                int vId = view.getId();
                LogMy.d(TAG, "In onTouch: " + vId);

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

                }
            }
        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception in TxnReportsCustActivity", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DIALOG_ERROR_NOTIFY);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);
        if (vId == R.id.btn_get_report) {
            startTxnFetch();
        }
    }*/

    private void startTxnFetch() {
        try {
            // clear old data
            //mWorkFragment.mAllFiles.clear();
            //mWorkFragment.mMissingFiles.clear();
            //mWorkFragment.mTxnsFromCsv.clear();
            if (mWorkFragment.mLastFetchTransactions != null) {
                mWorkFragment.mLastFetchTransactions.clear();
                mWorkFragment.mLastFetchTransactions = null;
            }
            mHelper.startTxnFetch(mFromDate, mToDate, mMerchantId, CustomerUser.getInstance().getCustomer().getPrivate_id(), false);
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is startTxnFetch", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DIALOG_ERROR_NOTIFY);
            //mWorkFragment.mTxnsFromCsv.clear();
        }
    }

    private void initDateInputs(Bundle instanceState) {
        if(instanceState==null) {
            // mFromDate as 'start of today' i.e. todayMidnight
            DateUtil now = new DateUtil(new Date(), TimeZone.getDefault());
            mFromDate = now.toMidnight().getTime();
            mToDate = new Date();
        } else {
            mFromDate = (Date)instanceState.getSerializable("mFromDate");
            mToDate = (Date)instanceState.getSerializable("mToDate");
        }
        mInputDateFrom.setText(mSdfOnlyDateDisplay.format(mFromDate));
        mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));

        mInputDateFrom.setOnTouchListener(this);
        mInputDateTo.setOnTouchListener(this);
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
                    .show(mFragMgr, DIALOG_ERROR_NOTIFY);
        } else {
            startTxnListFragment();
        }
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        AppCommonUtil.cancelProgressDialog(true);

        // Session timeout case - show dialog and logout - irrespective of invoked operation
        if(errorCode==ErrorCodes.SESSION_TIMEOUT || errorCode==ErrorCodes.NOT_LOGGED_IN) {
            setResult(ErrorCodes.SESSION_TIMEOUT, null);
            finish();
            return;
        }

        try {
            switch(operation) {
                case MyRetainedFragment.REQUEST_FETCH_TXNS:
                    if (errorCode == ErrorCodes.NO_ERROR ||
                            errorCode == ErrorCodes.NO_DATA_FOUND) {
                        mHelper.onDbTxnsAvailable(mWorkFragment.mLastFetchTransactions);
                    } else {
                        DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                                .show(getFragmentManager(), DIALOG_ERROR_NOTIFY);
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
                                .show(getFragmentManager(), DIALOG_ERROR_NOTIFY);
                    }
                    break;
            }

        } catch (Exception e) {
            LogMy.e(TAG, "Exception is ReportsActivity:onBgProcessResponse: "+operation+": "+errorCode, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DIALOG_ERROR_NOTIFY);
        }
    }

    /*@Override
    public void showMchntDetails(String mchntId) {
        MerchantDetailsDialog dialog = MerchantDetailsDialog.newInstance(mchntId);
        dialog.show(getFragmentManager(), DIALOG_MERCHANT_DETAILS);
    }

    @Override
    public void getMchntTxns(String id, String name) {
        finish();
        // start itself again
        Intent intent = new Intent( this, TxnReportsCustActivity.class );
        if(id!=null) {
            intent.putExtra(TxnReportsCustActivity.EXTRA_MERCHANT_ID, id);
        }
        if(name!=null) {
            intent.putExtra(TxnReportsCustActivity.EXTRA_MERCHANT_NAME, name);
        }
        startActivity(intent);
    }*/

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
        if(tag.equals(DIALOG_ERROR_NOTIFY)) {
            if(mMerchantId==null || mMerchantId.isEmpty()) {
                onBackPressed();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(!mWorkFragment.getResumeOk())
            return;

        int count = getFragmentManager().getBackStackEntryCount();
        LogMy.d(TAG, "In onBackPressed: " + count);
        try {
            if (count == 0) {
                super.onBackPressed();
            } else {
                getFragmentManager().popBackStackImmediate();

                if (mMerchantId == null || mMerchantId.isEmpty()) {
                    // Case when latest txns are directly shown
                    // i.e. the main layout to enter from, to dates - was not shown
                    super.onBackPressed();

                } else if (mFragmentContainer.getVisibility() == View.VISIBLE && count == 1) {
                    getSupportActionBar().setTitle("Transactions");
                    mMainLayout.setVisibility(View.VISIBLE);
                    mFragmentContainer.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in TxnReportsCustActivity", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DIALOG_ERROR_NOTIFY);
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

    private EditText mLabelInfo;
    private EditText mInputDateFrom;
    private EditText mInputDateTo;
    private View mMerchantLayout;
    private EditText mInputMerchant;
    private AppCompatButton mBtnGetReport;

    private LinearLayout mMainLayout;
    private FrameLayout mFragmentContainer;

    private void bindUiResources() {
        mLabelInfo = (EditText) findViewById(R.id.label_info);
        mInputDateFrom = (EditText) findViewById(R.id.input_date_from);
        mInputDateTo = (EditText) findViewById(R.id.input_date_to);
        mMerchantLayout = findViewById(R.id.layout_merchant);
        mInputMerchant = (EditText) findViewById(R.id.input_merchant);
        mBtnGetReport = (AppCompatButton) findViewById(R.id.btn_get_report);

        mMainLayout = (LinearLayout) findViewById(R.id.layout_report_main);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_report);
    }

    @Override
    protected void onResume() {
        LogMy.d(TAG, "In onResume: ");
        super.onResume();
        if(AppCommonUtil.getProgressDialogMsg()!=null) {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
        }
        if(getFragmentManager().getBackStackEntryCount()==0) {
            // no fragment in backstack - so flag wont get set by any fragment - so set it here
            // though this shud never happen - as CashbackActivity always have a fragment
            mWorkFragment.setResumeOk(true);
        }
    }

    @Override
    protected void onPause() {
        LogMy.d(TAG,"In onPause: ");
        super.onPause();
        mWorkFragment.setResumeOk(false);
        AppCommonUtil.cancelProgressDialog(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("mFromDate", mFromDate);
        outState.putSerializable("mToDate", mToDate);
        //outState.putInt("mDetailedTxnPos", mDetailedTxnPos);
        mWorkFragment.mTxnReportHelper = mHelper;
    }

}
