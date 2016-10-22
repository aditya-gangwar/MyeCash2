package in.myecash.merchantbase;

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
import android.widget.Toast;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.CommonUtils;
import in.myecash.common.CsvConverter;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.common.DateUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by adgangwa on 04-04-2016.
 */
public class ReportsActivity extends AppCompatActivity implements
        View.OnClickListener, MyRetainedFragment.RetainedFragmentIf,
        DatePickerDialog.DatePickerIf, TxnSummaryFragment.TxnSummaryFragmentIf,
        TxnListFragment.TxnListFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        TxnDetailsDialog.TxnDetailsDialogIf {
    private static final String TAG = "ReportsActivity";

    public static final String EXTRA_CUSTOMER_ID = "extraCustId";

    private static final String RETAINED_FRAGMENT = "retainedFragReports";
    private static final String DIALOG_DATE_FROM = "DialogDateFrom";
    private static final String DIALOG_DATE_TO = "DialogDateTo";
    private static final String TXN_LIST_FRAGMENT = "TxnListFragment";
    private static final String TXN_SUMMARY_FRAGMENT = "TxnSummaryFragment";

    // All required date formatters
    //private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateBackend = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
    //private SimpleDateFormat mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    FragmentManager mFragMgr;
    MyRetainedFragment mWorkFragment;
    private MerchantUser mMerchantUser;
    private DateUtil mToday;
    private String mCustomerId;

    // Store and restore as part of instance state
    private Date mFromDate;
    private Date mToDate;
    private int mDetailedTxnPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // gets handlers to screen resources
        bindUiResources();
        mMerchantUser = MerchantUser.getInstance();
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
        mInputCustId.setText(getIntent().getStringExtra(EXTRA_CUSTOMER_ID));

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
                minFrom.removeDays(MyGlobalSettings.getMchntReportHistoryDays());

                DialogFragment fromDialog = DatePickerDialog.newInstance(mFromDate, minFrom.getTime(), mToday.getTime());
                fromDialog.show(getFragmentManager(), DIALOG_DATE_FROM);

            } else if (vId == R.id.input_date_to) {
                if (mFromDate == null) {
                    Toast.makeText(this, "Set From date first", Toast.LENGTH_LONG).show();
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
                mCustomerId = mInputCustId.getText().toString();
                if (mCustomerId.length() > 0) {
                    if( mCustomerId.length() != CommonConstants.CUSTOMER_INTERNAL_ID_LEN &&
                            mCustomerId.length() != CommonConstants.MOBILE_NUM_LENGTH ) {
                        mInputCustId.setError(AppCommonUtil.getErrorDesc(ErrorCodes.INVALID_LENGTH));
                        return;
                    }
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
        // start thread to fetch data from DB
        // show progress dialog
        AppCommonUtil.showProgressDialog(ReportsActivity.this, AppConstants.progressReports);

        LogMy.d( TAG, String.valueOf(mToday.getTime().getTime()) +", "+ String.valueOf(mFromDate.getTime()) +", "+ String.valueOf(mToDate.getTime()) );

        // check if today's txns required
        if( mToday.getTime().getTime() == mFromDate.getTime() ) {
            if (mToday.getTime().getTime() == mToDate.getTime()) {
                // only today's txns are required, fetch from DB table
                mWorkFragment.fetchTransactions(buildWhereClause());
            } else {
                // invalid state: if from == today, then To has to be == today only
                LogMy.e(TAG,"ReportsActivity: Invalid state: From is today, but To is not");
                AppCommonUtil.cancelProgressDialog(true);
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
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
                mWorkFragment.fetchTxnFiles(ReportsActivity.this, mWorkFragment.mMissingFiles);
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

        // create summary and start fragment
        addToSummary(mWorkFragment.mLastFetchTransactions);
        startTxnSummaryFragment();
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
        String merchantId = MerchantUser.getInstance().getMerchantId();
        mWorkFragment.mMissingFiles.clear();

        for(int i=0; i<diffDays; i++) {
            String filename = CommonUtils.getTxnCsvFilename(txnDay.getTime(),merchantId);
            mWorkFragment.mAllFiles.add(filename);

            File file = getFileStreamPath(filename);
            if(file == null || !file.exists()) {
                // file does not exist
                LogMy.d(TAG,"Missing file: "+filename);
                String filepath = CommonUtils.getMerchantTxnDir(merchantId) + CommonConstants.FILE_PATH_SEPERATOR + filename;
                mWorkFragment.mMissingFiles.add(filepath);
            }
            txnDay.addDays(1);
        }
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // customer and merchant id
        whereClause.append("merchant_id = '").append(mMerchantUser.getMerchantId()).append("'");

        //DateUtil from = new DateUtil(mFromDate);
        //from.toMidnight();
        // Increment by 1 day, and then take midnight
        DateUtil to = new DateUtil(mToDate, TimeZone.getDefault());
        to.addDays(1);
        to.toMidnight();

        whereClause.append(" AND create_time >= '").append(mFromDate.getTime()).append("'");
        // we used '<' and not '<='
        whereClause.append(" AND create_time < '").append(to.getTime().getTime()).append("'");

        /*
        String fromDateStr = mSdfOnlyDateBackend.format(mFromDate);
        whereClause.append(" AND create_time >= '").append(fromDateStr).append("'");

        // increment to date by 1 day
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mToDate);
        calendar.add(Calendar.DATE, 1);
        String toDateStr = mSdfOnlyDateBackend.format(calendar.getTime());
        // we used '<' and not '<='
        whereClause.append(" AND create_time < '").append(toDateStr).append("'");*/

        if(mCustomerId.length() == CommonConstants.MOBILE_NUM_LENGTH) {
            whereClause.append(" AND customer_id = '").append(mCustomerId).append("'");
        } else if(mCustomerId.length() == CommonConstants.CUSTOMER_INTERNAL_ID_LEN) {
            whereClause.append(" AND cust_private_id = '").append(mCustomerId).append("'");
        }

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


    @Override
    public void onDateSelected(Date argDate, String tag) {
        LogMy.d(TAG, "Selected date: " + argDate.toString());
        //SimpleDateFormat format = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, AppConstants.DATE_LOCALE);
        if(tag.equals(DIALOG_DATE_FROM)) {
            mFromDate = argDate;
            mInputDateFrom.setText(mSdfOnlyDateDisplay.format(mFromDate));
        } else {
            mToDate = argDate;
            mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));
        }
    }

    private void initToolbar() {
        LogMy.d(TAG, "In initToolbar");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_report);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

        /*
        if(isNewActivity) {
            getSupportActionBar().setTitle("History");
        }*/
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
                case MyRetainedFragment.REQUEST_IMAGE_DOWNLOAD:
                    AppCommonUtil.cancelProgressDialog(true);
                    if (errorCode != ErrorCodes.NO_ERROR ||
                            mWorkFragment.mLastFetchedImage==null) {
                        AppCommonUtil.toast(this, "Failed to download image file");
                    }
                    // re-open the details dialog - but only if 'txn list fragment' is still open
                    TxnListFragment fragment = (TxnListFragment)mFragMgr.findFragmentByTag(TXN_SUMMARY_FRAGMENT);
                    if (fragment != null) {
                        fragment.showDetailedDialog(mDetailedTxnPos);
                    } else {
                        LogMy.d(TAG,"Txn list fragment not available, ignoring downloaded file");
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

    @Override
    public void showTxnImg(int currTxnPos) {
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mDetailedTxnPos = currTxnPos;
        String txnImgFileName = mWorkFragment.mLastFetchTransactions.get(currTxnPos).getImgFileName();
        String mchntId = mWorkFragment.mLastFetchTransactions.get(currTxnPos).getMerchant_id();

        String url = CommonUtils.getTxnImgDir(mchntId)+txnImgFileName;
        mWorkFragment.fetchImageFile(url);
    }

    // process all files in 'mAllFiles' and add applicable CSV records in mWorkFragment.mFilteredCsvRecords
    private void processFiles() throws Exception {
        boolean isCustomerFilter = false;
        if(mCustomerId != null && mCustomerId.length() > 0 )
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
                mCustomerId.equals(txn.getCustomer_id()) ||
                mCustomerId.equals(txn.getCust_private_id()) ) {

            mWorkFragment.mTxnsFromCsv.add(txn);
        }

        /*
        if(!isCustomerFilter ||
                mCustomerId.equals(csvFields[CommonConstants.TXN_CSV_IDX_CUSTOMER_ID]) ) {
            // convert csv record to Transaction object and add to list
            mWorkFragment.mTxnsFromCsv.add(MyTransaction.getTxnFromCsv(csvFields));
        }*/
    }

    private void addToSummary(List<Transaction> txns) {
        LogMy.d(TAG, "In addToSummary: "+txns.size());
        int summary[] = mWorkFragment.mSummary;

        // reset first
        summary[AppConstants.INDEX_TXN_COUNT] = 0;
        summary[AppConstants.INDEX_BILL_AMOUNT] = 0;
        summary[AppConstants.INDEX_ADD_ACCOUNT] = 0;
        summary[AppConstants.INDEX_DEBIT_ACCOUNT] = 0;
        summary[AppConstants.INDEX_CASHBACK] = 0;
        summary[AppConstants.INDEX_DEBIT_CASHBACK] = 0;

        for (Transaction txn : txns) {
            summary[AppConstants.INDEX_TXN_COUNT]++;
            summary[AppConstants.INDEX_BILL_AMOUNT] = summary[AppConstants.INDEX_BILL_AMOUNT] + txn.getTotal_billed();
            summary[AppConstants.INDEX_ADD_ACCOUNT] = summary[AppConstants.INDEX_ADD_ACCOUNT] + txn.getCl_credit();
            summary[AppConstants.INDEX_DEBIT_ACCOUNT] = summary[AppConstants.INDEX_DEBIT_ACCOUNT] + txn.getCl_debit();
            summary[AppConstants.INDEX_CASHBACK] = summary[AppConstants.INDEX_CASHBACK] + txn.getCb_credit();
            summary[AppConstants.INDEX_DEBIT_CASHBACK] = summary[AppConstants.INDEX_DEBIT_CASHBACK] + txn.getCb_debit();
        }
    }

    private void startTxnSummaryFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(TXN_SUMMARY_FRAGMENT);
        if (fragment == null) {
            LogMy.d(TAG,"Creating new txn summary fragment");

            // Create new fragment and transaction
            fragment = TxnSummaryFragment.newInstance(mWorkFragment.mSummary);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Add over the existing fragment
            mMainLayout.setVisibility(View.GONE);
            mFragmentContainer.setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragment_container_report, fragment, TXN_SUMMARY_FRAGMENT);
            transaction.addToBackStack(TXN_SUMMARY_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public void showTxnDetails() {
        startTxnListFragment();
    }

    private void startTxnListFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(TXN_LIST_FRAGMENT);
        if (fragment == null) {
            LogMy.d(TAG,"Creating new txn list fragment");

            // Create new fragment and transaction
            //fragment = new TxnListFragment_2();
            fragment = TxnListFragment.getInstance(mFromDate,mToDate);
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

            if(mFragmentContainer.getVisibility()==View.VISIBLE && count==1) {
                getSupportActionBar().setTitle("History");
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
    private EditText mInputCustId;
    private AppCompatButton mBtnGetReport;

    private LinearLayout mMainLayout;
    private FrameLayout mFragmentContainer;

    private void bindUiResources() {
        mInputDateFrom = (EditText) findViewById(R.id.input_date_from);
        mInputDateTo = (EditText) findViewById(R.id.input_date_to);
        mInputCustId = (EditText) findViewById(R.id.input_customer_id);
        mBtnGetReport = (AppCompatButton) findViewById(R.id.btn_get_report);

        mMainLayout = (LinearLayout) findViewById(R.id.layout_report_main);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_report);
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        TxnListFragment fragment = (TxnListFragment)mFragMgr.findFragmentByTag(TXN_LIST_FRAGMENT);
        if (fragment != null) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }*/

    @Override
    protected void onResume() {
        LogMy.d(TAG, "In onResume: ");
        super.onResume();

        /*
        getSupportActionBar().setTitle("History");
        mMainLayout.setVisibility(View.VISIBLE);
        mFragmentContainer.setVisibility(View.GONE);*/

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

    /*
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }*/

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("mFromDate", mFromDate);
        outState.putSerializable("mToDate", mToDate);
        outState.putInt("mDetailedTxnPos", mDetailedTxnPos);
    }

}
