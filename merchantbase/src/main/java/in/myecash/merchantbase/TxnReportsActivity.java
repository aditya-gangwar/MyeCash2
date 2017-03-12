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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import in.myecash.appbase.BaseActivity;
import in.myecash.appbase.MyDatePickerDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.TxnReportsHelper;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.common.DateUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by adgangwa on 04-04-2016.
 */
public class TxnReportsActivity extends BaseActivity implements
        MyRetainedFragment.RetainedFragmentIf,
        MyDatePickerDialog.MyDatePickerIf, TxnSummaryFragment.TxnSummaryFragmentIf,
        TxnListFragment.TxnListFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        TxnDetailsDialog.TxnDetailsDialogIf, TxnReportsHelper.TxnReportsHelperIf,
        TxnCancelDialog.TxnCancelDialogIf, TxnPinInputDialog.TxnPinInputDialogIf,
        CustomerDetailsDialog.CustomerDetailsDialogIf {
    private static final String TAG = "MchntApp-TxnReportsActivity";

    public static final String EXTRA_CUSTOMER_ID = "extraCustId";

    private static final String RETAINED_FRAGMENT = "retainedFragReports";
    private static final String DIALOG_DATE_FROM = "DialogDateFrom";
    private static final String DIALOG_DATE_TO = "DialogDateTo";
    private static final String TXN_LIST_FRAGMENT = "TxnListFragment";
    private static final String TXN_SUMMARY_FRAGMENT = "TxnSummaryFragment";
    private static final String DIALOG_TXN_CANCEL_CONFIRM = "dialogTxnCanConf";
    private static final String DIALOG_PIN_CANCEL_TXN = "dialogPinCanTxn";
    private static final String DIALOG_CUSTOMER_DETAILS = "dialogCustomerDetails";

    // All required date formatters
    private SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    FragmentManager mFragMgr;
    MyRetainedFragment mWorkFragment;

    //private Date mNow;
    //private Date mTodayEoD;
    private String mCustomerId;

    // Store and restore as part of instance state
    private TxnReportsHelper mHelper;
    private Date mFromDate;
    private Date mToDate;
    private int mLastTxnPos;
    private String mCancelTxnId;
    private String mCancelTxnCardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreate");
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
        //mNow = new Date();
        // end of today
        //DateUtil now = new DateUtil(new Date(), TimeZone.getDefault());
        //mTodayEoD = now.toEndOfDay().getTime();
        //LogMy.d( TAG, "Now: "+String.valueOf(now.getTime()) +", TodayEoD: "+ String.valueOf(mTodayEoD.getTime()) );

        // create helper instance
        if(savedInstanceState==null) {
            mHelper = new TxnReportsHelper(this);
        } else {
            mHelper = mWorkFragment.mTxnReportHelper;
        }

        initToolbar();
        initDateInputs(savedInstanceState);
        String txt = String.format(getString(R.string.mchnt_txn_history_info), MyGlobalSettings.getMchntTxnHistoryDays().toString() );
        mLabelInfo.setText(txt);

        mBtnGetReport.setOnClickListener(this);
        if(savedInstanceState!=null) {
            mLastTxnPos = savedInstanceState.getInt("mLastTxnPos");
            mCancelTxnId = savedInstanceState.getString("mCancelTxnId");
            mCancelTxnCardId = savedInstanceState.getString("mCancelTxnCardId");
        } else {
            mLastTxnPos = -1;
        }
        mInputCustId.setText(getIntent().getStringExtra(EXTRA_CUSTOMER_ID));

        mInputCustId.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                return false;
            }
        });

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
                minFrom.removeDays(MyGlobalSettings.getMchntTxnHistoryDays());

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
            LogMy.e(TAG, "Exception in TxnReportsActivity:onTouch", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
        return true;
    }

    @Override
    public void handleBtnClick(View v) {
        if(!mWorkFragment.getResumeOk()) {
            return;
        }

        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        try {
            if (vId == R.id.btn_get_report) {
                // clear old data
                //mWorkFragment.mAllFiles.clear();
                //mWorkFragment.mMissingFiles.clear();
                //mWorkFragment.mTxnsFromCsv.clear();
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

                //fetchReportData();
                mHelper.startTxnFetch(mFromDate, mToDate,
                        MerchantUser.getInstance().getMerchantId(), mCustomerId);
            }
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is ReportsActivity:onClick: "+vId, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    /*@Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_UP) {
            try {
                if(!mWorkFragment.getResumeOk()) {
                    return true;
                }

                int vId = v.getId();
                LogMy.d(TAG, "In onTouch: " + vId);

                if (vId == R.id.input_date_from) {
                    // Find the minimum date for DatePicker
                    DateUtil minFrom = new DateUtil(new Date(), TimeZone.getDefault());
                    minFrom.removeDays(MyGlobalSettings.getMchntTxnHistoryDays());

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
            } catch (Exception e) {
                AppCommonUtil.cancelProgressDialog(true);
                LogMy.e(TAG, "Exception in TxnReportsActivity:onTouch", e);
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if(!mWorkFragment.getResumeOk()) {
            return;
        }

        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        try {
            if (vId == R.id.btn_get_report) {
                // clear old data
                //mWorkFragment.mAllFiles.clear();
                //mWorkFragment.mMissingFiles.clear();
                //mWorkFragment.mTxnsFromCsv.clear();
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

                //fetchReportData();
                mHelper.startTxnFetch(mFromDate, mToDate,
                        MerchantUser.getInstance().getMerchantId(), mCustomerId);
            }
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is ReportsActivity:onClick: "+vId, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }*/

    private void initDateInputs(Bundle instanceState) {
        if(instanceState==null) {
            // mFromDate as 'start of today' i.e. todayMidnight
            DateUtil now = new DateUtil(new Date(), TimeZone.getDefault());
            mFromDate = now.toMidnight().getTime();

            //DateUtil now2 = new DateUtil(new Date(), TimeZone.getDefault());
            //mToDate = now2.toEndOfDay().getTime();
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
        //SimpleDateFormat format = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, AppConstants.DATE_LOCALE);
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
            // create summary and start fragment
            addToSummary(mWorkFragment.mLastFetchTransactions);
            startTxnSummaryFragment();
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
                case MyRetainedFragment.REQUEST_IMAGE_DOWNLOAD:
                    if (errorCode != ErrorCodes.NO_ERROR ||
                            mWorkFragment.mLastFetchedImage==null) {
                        AppCommonUtil.toast(this, "Failed to download image file");
                    }
                    // re-open the details dialog - but only if 'txn list fragment' is still open
                    TxnListFragment fragment = (TxnListFragment)mFragMgr.findFragmentByTag(TXN_SUMMARY_FRAGMENT);
                    if (fragment != null) {
                        fragment.showDetailedDialog(mLastTxnPos);
                    } else {
                        LogMy.d(TAG,"Txn list fragment not available, ignoring downloaded file");
                    }
                    break;
                case MyRetainedFragment.REQUEST_CANCEL_TXN:
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        // txn cancel success - refresh the list
                        // update old list for new cancelled txn
                        Transaction cancelledTxn = mWorkFragment.mCurrTransaction.getTransaction();
                        if(mWorkFragment.mLastFetchTransactions.get(mLastTxnPos).getTrans_id().equals(cancelledTxn.getTrans_id())) {
                            mWorkFragment.mLastFetchTransactions.remove(mLastTxnPos);
                            mWorkFragment.mLastFetchTransactions.add(cancelledTxn);
                            startTxnListFragment();

                            // if required, start upload of txn image file in background thread
                            if(mWorkFragment.mCardImageFilename != null) {
                                mWorkFragment.uploadImageFile(this, mWorkFragment.mCardImageFilename,
                                        cancelledTxn.getCanImgFileName(),
                                        CommonUtils.getTxnImgDir(new Date()));
                                mWorkFragment.mCardImageFilename = null;
                            }

                        } else {
                            // some logic issue - I shudn't be here - raise alarm
                            LogMy.e(TAG,"Current and Cancelled Txns are not same");
                            Map<String,String> params = new HashMap<>();
                            params.put("currentTxnID",mWorkFragment.mLastFetchTransactions.get(mLastTxnPos).getTrans_id());
                            params.put("cancelledTxnID",cancelledTxn.getTrans_id());
                            AppAlarms.wtf(MerchantUser.getInstance().getMerchantId(), DbConstants.USER_TYPE_MERCHANT,
                                    "TxnReportsActivity:onBgProcessResponse:REQUEST_CANCEL_TXN",params);
                        }

                    } else {
                        // delete file, if available
                        if(mWorkFragment.mCardImageFilename != null) {
                            deleteFile(mWorkFragment.mCardImageFilename);
                            mWorkFragment.mCardImageFilename = null;
                        }
                        DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    }
                    break;

                case MyRetainedFragment.REQUEST_UPLOAD_IMG:
                    if(errorCode==ErrorCodes.NO_ERROR) {
                        LogMy.d(TAG,"Uploaded image file successfully");
                    } else {
                        LogMy.e(TAG,"Failed to upload image file");
                        //raise alarm
                        Map<String,String> params = new HashMap<>();
                        params.put("opCode",String.valueOf(MyRetainedFragment.REQUEST_UPLOAD_IMG));
                        params.put("erroCode",String.valueOf(errorCode));
                        AppAlarms.fileUploadFailed(MerchantUser.getInstance().getMerchantId(),
                                DbConstants.USER_TYPE_MERCHANT,"onBgProcessResponse",params);
                    }
                    break;

                case MyRetainedFragment.REQUEST_GET_CASHBACK:
                    onCashbackResponse(errorCode);
                    break;
            }

        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception is ReportsActivity:onBgProcessResponse: "+operation+": "+errorCode, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    // This fx. gets called in case of successfull customer registration too
    public void onCashbackResponse(int errorCode) {
        LogMy.d(TAG, "In onCashbackResponse: " + errorCode);

        AppCommonUtil.cancelProgressDialog(true);
        // response against search of particular customer details
        if(errorCode==ErrorCodes.NO_ERROR) {
            // show customer details dialog
            CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(-1, false);
            dialog.show(mFragMgr, DIALOG_CUSTOMER_DETAILS);
        } else {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void showCustDetails(String internalId) {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.fetchCashback(internalId);
        }
    }

    @Override
    public void showTxnImg(int currTxnPos) {
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mLastTxnPos = currTxnPos;
        String txnImgFileName = mWorkFragment.mLastFetchTransactions.get(currTxnPos).getImgFileName();
        Date txnCreated = mWorkFragment.mLastFetchTransactions.get(currTxnPos).getCreate_time();
        //String mchntId = mWorkFragment.mLastFetchTransactions.get(currTxnPos).getMerchant_id();

        String url = CommonUtils.getTxnImgDir(txnCreated)+CommonConstants.FILE_PATH_SEPERATOR+txnImgFileName;
        mWorkFragment.fetchImageFile(url);
    }

    @Override
    public void getCustTxns(String id) {
        // Do nothing - shudn't get called as 'Get Txns' button is not shown
        // for 'customer details' dialog - when shown from 'Txn details' dialog
    }

    @Override
    public void cancelTxn(int txnPos) {
        // show confirmation dialog - and ask for card scan
        mLastTxnPos = txnPos;
        Transaction txn = mWorkFragment.mLastFetchTransactions.get(txnPos);
        mWorkFragment.mCurrTransaction = new MyTransaction(txn);

        TxnCancelDialog dialog = TxnCancelDialog.newInstance(txn);
        dialog.show(mFragMgr, DIALOG_TXN_CANCEL_CONFIRM);
    }

    @Override
    public void onCancelTxnConfirm(String txnId, String cardId, String imgFileName) {
        mCancelTxnId = txnId;
        mCancelTxnCardId = cardId;
        mWorkFragment.mCardImageFilename = imgFileName;

        // ask for customer PIN
        TxnPinInputDialog dialog = TxnPinInputDialog.newInstance(0,0,0,txnId);
        dialog.show(mFragMgr, DIALOG_PIN_CANCEL_TXN);
    }

    @Override
    public void onTxnPin(String pin, String tag) {
        if(tag.equals(DIALOG_PIN_CANCEL_TXN)) {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.cancelTxn(mCancelTxnId, mCancelTxnCardId, pin);
        }
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
            summary[AppConstants.INDEX_ADD_ACCOUNT] = summary[AppConstants.INDEX_ADD_ACCOUNT] + txn.getCl_credit();

            if(txn.getCancelTime()==null) {
                summary[AppConstants.INDEX_BILL_AMOUNT] = summary[AppConstants.INDEX_BILL_AMOUNT] + txn.getTotal_billed();
                summary[AppConstants.INDEX_DEBIT_ACCOUNT] = summary[AppConstants.INDEX_DEBIT_ACCOUNT] + txn.getCl_debit();
                summary[AppConstants.INDEX_CASHBACK] = summary[AppConstants.INDEX_CASHBACK] + txn.getCb_credit() + txn.getExtra_cb_credit();
                summary[AppConstants.INDEX_DEBIT_CASHBACK] = summary[AppConstants.INDEX_DEBIT_CASHBACK] + txn.getCb_debit();
            }
        }
    }

    private void startTxnSummaryFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(TXN_SUMMARY_FRAGMENT);
        if (fragment == null) {
            LogMy.d(TAG,"Creating new txn summary fragment");

            // Create new fragment and transaction
            fragment = TxnSummaryFragment.newInstance(mWorkFragment.mSummary,
                    mSdfOnlyDateDisplay.format(mFromDate),
                    mSdfOnlyDateDisplay.format(mToDate));
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
        } else {
            // update the UI
            TxnListFragment txnListFrag = (TxnListFragment) fragment;
            txnListFrag.sortNupdateUI();
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

        if(!mWorkFragment.getResumeOk())
            return;

        try {
            if (count == 0) {
                super.onBackPressed();
            } else {
                getFragmentManager().popBackStackImmediate();

                if(mFragmentContainer.getVisibility()==View.VISIBLE && count==1) {
                    setToolbarTitle("Transactions");
                    mMainLayout.setVisibility(View.VISIBLE);
                    mFragmentContainer.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception in TxnReportsActivity:onBackPressed", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
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
    private EditText mInputCustId;
    private AppCompatButton mBtnGetReport;

    private LinearLayout mMainLayout;
    private FrameLayout mFragmentContainer;

    private void bindUiResources() {
        mLabelInfo = (EditText) findViewById(R.id.label_info);
        mInputDateFrom = (EditText) findViewById(R.id.input_date_from);
        mInputDateTo = (EditText) findViewById(R.id.input_date_to);
        mInputCustId = (EditText) findViewById(R.id.input_customer_id);
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
        AppCommonUtil.cancelToast();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogMy.d(TAG, "In onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putSerializable("mFromDate", mFromDate);
        outState.putSerializable("mToDate", mToDate);
        outState.putInt("mLastTxnPos", mLastTxnPos);
        outState.putString("mCancelTxnId",mCancelTxnId);
        outState.putString("mCancelTxnCardId",mCancelTxnCardId);
        mWorkFragment.mTxnReportHelper = mHelper;
    }

    @Override
    public void onBgThreadCreated() {

    }
}
