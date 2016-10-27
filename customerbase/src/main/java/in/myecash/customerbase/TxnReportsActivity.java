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
import java.util.TimeZone;

import in.myecash.appbase.DatePickerDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
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
public class TxnReportsActivity extends AppCompatActivity implements
        View.OnClickListener, MyRetainedFragment.RetainedFragmentIf,
        DatePickerDialog.DatePickerIf, TxnListFragment.TxnListFragmentIf,
        DialogFragmentWrapper.DialogFragmentWrapperIf, TxnDetailsDialog.TxnDetailsDialogIf {
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
    //private CustomerUser mCustomerUser;
    private DateUtil mToday;
    private String mMerchantId;
    private String mMerchantName;
    private String mCustPvtId;

    // Store and restore as part of instance state
    private Date mFromDate;
    private Date mToDate;
    private int mDetailedTxnPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txn_report);

        // gets handlers to screen resources
        bindUiResources();
        mCustPvtId = CustomerUser.getInstance().getCustomer().getPrivate_id();
        mToday = new DateUtil(new Date(), TimeZone.getDefault());
        mToday.toMidnight();

        initToolbar();
        initDateInputs(savedInstanceState);

        mBtnGetReport.setOnClickListener(this);
        if(savedInstanceState!=null) {
            mDetailedTxnPos = savedInstanceState.getInt("mDetailedTxnPos");
        } else {
            mDetailedTxnPos = -1;
        }


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
    }

    @Override
    public void onBgThreadCreated() {
        // was facing race condition - where mBtnGetReport.performClick() was getting called
        // before bg thread is initialized properly
        // so added this callback

        // if 'Merchant ID' not provided - means fetch only latest txns from DB table
        if(mMerchantId==null || mMerchantId.isEmpty()) {
            DateUtil from = new DateUtil(new Date(), TimeZone.getDefault());
            // -1 as 'today' is inclusive in 'cust txn keep days'
            from.removeDays(MyGlobalSettings.getCustTxnKeepDays()-1);
            from.toMidnight();
            mFromDate = from.getTime();
            mToDate = mToday.getTime();
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
                //int oldDays = (Integer) MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_REPORTS_HISTORY_DAYS);
                DateUtil minFrom = new DateUtil(new Date(), TimeZone.getDefault());
                minFrom.removeDays(MyGlobalSettings.getCustTxnHistoryDays());

                DialogFragment fromDialog = DatePickerDialog.newInstance(mFromDate, minFrom.getTime(), mToday.getTime());
                fromDialog.show(getFragmentManager(), DIALOG_DATE_FROM);

            } else if (vId == R.id.input_date_to) {
                if (mFromDate == null) {
                    AppCommonUtil.toast(this, "Set From Date");
                } else {
                    DialogFragment toDialog = DatePickerDialog.newInstance(mToDate, mFromDate, mToday.getTime());
                    toDialog.show(getFragmentManager(), DIALOG_DATE_TO);
                }

            } else if (vId == R.id.btn_get_report) {
                // clear old data
                mWorkFragment.mAllFiles.clear();
                mWorkFragment.mMissingFiles.clear();
                mWorkFragment.mTxnsFromCsv.clear();
                if (mWorkFragment.mLastFetchTransactions != null) {
                    mWorkFragment.mLastFetchTransactions.clear();
                    mWorkFragment.mLastFetchTransactions = null;
                }
                fetchReportData();

            } else {
            }
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is ReportsActivity:onClick: "+vId, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            mWorkFragment.mTxnsFromCsv.clear();
        }
    }

    private void fetchReportData() throws Exception{
        // show progress dialog
        AppCommonUtil.showProgressDialog(TxnReportsActivity.this, AppConstants.progressReports);

        LogMy.d( TAG, String.valueOf(mToday.getTime().getTime()) +", "+ String.valueOf(mFromDate.getTime()) +", "+ String.valueOf(mToDate.getTime()) );

        // check if today's txns required
        if( mToday.getTime().getTime() == mFromDate.getTime() ) {
            if (mToday.getTime().getTime() == mToDate.getTime()) {
                // only today's txns are required, fetch from DB table
                mWorkFragment.fetchTransactions(buildWhereClause());
            } else {
                // invalid state: if from == today, then To has to be == today only
                LogMy.e(TAG,"Invalid state: From is today, but To is not");
                AppCommonUtil.cancelProgressDialog(true);
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        } else {
            if(mMerchantId==null || mMerchantId.isEmpty()) {
                // fetch only txns available in DB table
                mWorkFragment.fetchTransactions(buildWhereClause());
            } else {
                // older txns are required
                // may be today's txns are also required - will be checked once all old txns are in memory

                // find which txn csv files are not locally available
                getTxnCsvFilePaths();
                if (mWorkFragment.mMissingFiles.isEmpty()) {
                    // all required txn files are locally available
                    // process all files and store applicable CSV records in 'mWorkFragment.mFilteredCsvRecords'
                    onAllTxnFilesAvailable(false);
                } else {
                    // one or more txn files are not locally available, fetch the same from backend
                    // all files will be processed in single go, after fetching missing files
                    mWorkFragment.fetchTxnFiles(TxnReportsActivity.this, mWorkFragment.mMissingFiles);
                }
            }
        }
    }

    // this function is called for further processing,
    // when all CSV txn files for old days are available locally
    private void onAllTxnFilesAvailable(boolean remoteCsvTxnFileNotFound) throws Exception {
        // process CSV files to extract applicable CSV records
        processFiles();
        // check if records from DB table are to be fetched too
        if( mToday.getTime().getTime() == mToDate.getTime() ||
                remoteCsvTxnFileNotFound ) {
            mWorkFragment.fetchTransactions(buildWhereClause());
        } else {
            // no DB table records to be fecthed
            generateReport();
        }
    }

    private void generateReport() {
        // all txns available now - either from files or DB or both.

        AppCommonUtil.cancelProgressDialog(true);

        // make 'mWorkFragment.mLastFetchTransactions' refer to final list of txns
        if( !mWorkFragment.mTxnsFromCsv.isEmpty() &&
                mWorkFragment.mLastFetchTransactions != null &&
                !mWorkFragment.mLastFetchTransactions.isEmpty() ) {
            LogMy.d(TAG,"Merging records from CSV and DB: "+mWorkFragment.mTxnsFromCsv.size()+", "+mWorkFragment.mLastFetchTransactions.size());
            // merge if both type of records available
            mWorkFragment.mLastFetchTransactions.addAll(mWorkFragment.mTxnsFromCsv);

        } else if( mWorkFragment.mLastFetchTransactions == null ||
                mWorkFragment.mLastFetchTransactions.isEmpty()) {
            LogMy.d(TAG,"Only records from CSV available: "+mWorkFragment.mTxnsFromCsv.size());
            mWorkFragment.mLastFetchTransactions = mWorkFragment.mTxnsFromCsv;

        } else {
            LogMy.d(TAG,"Only records from DB available: "+mWorkFragment.mLastFetchTransactions.size());
        }

        if(mWorkFragment.mLastFetchTransactions.isEmpty()) {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.NO_DATA_FOUND), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            return;
        }

        // Remove duplicates - this may happen due to some error in backend archive process
        // which can result same txns to be present in both the DB table and CSV file
        // A simpler way is to override equals() and hashcode() methods of Transaction class
        // however didn't want to loose the ability to export class from Backendless and use as it is
        mWorkFragment.mLastFetchTransactions = new ArrayList<>(MyTransaction.removeDuplicateTxns(mWorkFragment.mLastFetchTransactions));

        // create summary and start fragment
        //addToSummary(mWorkFragment.mLastFetchTransactions);
        //startTxnSummaryFragment();
        startTxnListFragment();
    }

    // checks for txn files locally and sets 'mWorkFragment.mAllFiles' and 'mWorkFragment.mMissingFiles' accordingly
    private void getTxnCsvFilePaths() {
        // loop through the dates for which report is required
        // check if file against the date is available locally
        // if not, add in missing file list
        long diff = Math.abs(mToDate.getTime() - mFromDate.getTime());
        int diffDays = (int) (diff / (24 * 60 * 60 * 1000));

        // If end date is of today - don't consider the end day - as no file is created for today
        if(mToDate.getTime()!=mToday.getTime().getTime()) {
            // Add 1 as both days are inclusive
            diffDays = diffDays + 1;
        }

        DateUtil txnDay = new DateUtil(mFromDate, TimeZone.getDefault());
        mWorkFragment.mMissingFiles.clear();

        for(int i=0; i<diffDays; i++) {
            String filename = CommonUtils.getTxnCsvFilename(txnDay.getTime(),mMerchantId);
            mWorkFragment.mAllFiles.add(filename);

            File file = getFileStreamPath(filename);
            if(file == null || !file.exists()) {
                // file does not exist
                LogMy.d(TAG,"Missing file: "+filename);
                String filepath = CommonUtils.getMerchantTxnDir(mMerchantId) + CommonConstants.FILE_PATH_SEPERATOR + filename;
                mWorkFragment.mMissingFiles.add(filepath);
            }
            txnDay.addDays(1);
        }
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // customer and merchant id
        whereClause.append("cust_private_id = '").append(mCustPvtId).append("'");
        if(mMerchantId!=null && !mMerchantId.isEmpty()) {
            whereClause.append("AND merchant_id = '").append(mMerchantId).append("'");
        }

        //DateUtil from = new DateUtil(mFromDate);
        //from.toMidnight();
        // Increment by 1 day, and then take midnight
        DateUtil to = new DateUtil(mToDate, TimeZone.getDefault());
        to.addDays(1);
        to.toMidnight();

        whereClause.append(" AND create_time >= '").append(mFromDate.getTime()).append("'");
        // we used '<' and not '<='
        whereClause.append(" AND create_time < '").append(to.getTime().getTime()).append("'");

        whereClause.append(" AND archived = false");

        return whereClause.toString();
    }

    private void initDateInputs(Bundle instanceState) {
        if(instanceState==null) {
            mFromDate = new Date(mToday.getTime().getTime());
            mToDate = new Date(mToday.getTime().getTime());
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
            mToDate = argDate;
            mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));
        }
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        try {
            switch(operation) {
                case MyRetainedFragment.REQUEST_FETCH_TXNS:
                    if (errorCode == ErrorCodes.NO_ERROR ||
                            errorCode == ErrorCodes.NO_DATA_FOUND) {
                        generateReport();
                    } else {
                        AppCommonUtil.cancelProgressDialog(true);
                        DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    }
                    break;

                case MyRetainedFragment.REQUEST_FETCH_TXN_FILES:
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        // all files should now be available locally
                        onAllTxnFilesAvailable(false);
                    } else if (errorCode == ErrorCodes.FILE_NOT_FOUND) {
                        // one or more files not found, may be corresponding day txns are present in table, try to fetch the same
                        onAllTxnFilesAvailable(true);
                    } else {
                        AppCommonUtil.cancelProgressDialog(true);
                        DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    }
                    break;
            }

        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception is ReportsActivity:onBgProcessResponse: "+operation+": "+errorCode, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            mWorkFragment.mTxnsFromCsv.clear();
        }
    }

    // process all files in 'mAllFiles' and add applicable CSV records in mWorkFragment.mFilteredCsvRecords
    private void processFiles() throws Exception {
        boolean isCustomerFilter = false;
        if(mCustPvtId != null && mCustPvtId.length() > 0 )
        {
            isCustomerFilter = true;
        }

        for(int i=0; i<mWorkFragment.mAllFiles.size(); i++) {
            try {
                InputStream inputStream = openFileInput(mWorkFragment.mAllFiles.get(i));

                if ( inputStream != null ) {
                    int lineCnt = 0;

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "";
                    while ( (receiveString = bufferedReader.readLine()) != null ) {
                        lineCnt++;
                        switch(lineCnt) {
                            case 1:
                                // this is header - do nothing
                                break;
                            default:
                                if(!receiveString.equals(CommonConstants.CSV_NEWLINE)) {
                                    processTxnCsvRecord(receiveString, isCustomerFilter);
                                }
                                break;
                        }
                    }
                    inputStream.close();
                    LogMy.d(TAG,mWorkFragment.mAllFiles.get(i)+": "+lineCnt);
                } else {
                    String error = "openFileInput returned null for txn CSV file: "+mWorkFragment.mAllFiles.get(i);
                    LogMy.e(TAG, error);
                    throw new FileNotFoundException(error);
                }
            }
            catch (FileNotFoundException fnf) {
                // ignore it - if the file already in 'missing list'
                boolean fileAlreadyMissing = false;
                String missingFile = mWorkFragment.mAllFiles.get(i);
                for (String curVal : mWorkFragment.mMissingFiles){
                    if (curVal.endsWith(missingFile)){
                        fileAlreadyMissing = true;
                    }
                }
                if(fileAlreadyMissing) {
                    // file not found in backend also - so ignore the exception
                    LogMy.i(TAG,"File not found locally, but is not found in backend also: "+missingFile);
                } else {
                    LogMy.e(TAG,"Txn CSV file not found locally: "+missingFile);
                    throw fnf;
                }
            }
        }
    }

    private void processTxnCsvRecord(String csvString, boolean isCustomerFilter)  throws ParseException {
        //String[] csvFields = csvString.split(CommonConstants.CSV_DELIMETER);
        Transaction txn = CsvConverter.txnFromCsvStr(csvString);

        if( !isCustomerFilter ||
                mCustPvtId.equals(txn.getCust_private_id()) ) {

            mWorkFragment.mTxnsFromCsv.add(txn);
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
            getFragmentManager().popBackStackImmediate();

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

        if(AppCommonUtil.getProgressDialogMsg()!=null) {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
        }
    }

    @Override
    protected void onPause() {
        LogMy.d(TAG,"In onPause: ");
        super.onPause();
        AppCommonUtil.cancelProgressDialog(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("mFromDate", mFromDate);
        outState.putSerializable("mToDate", mToDate);
        outState.putInt("mDetailedTxnPos", mDetailedTxnPos);
    }

}
