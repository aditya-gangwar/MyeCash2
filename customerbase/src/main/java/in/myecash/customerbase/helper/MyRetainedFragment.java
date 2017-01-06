package in.myecash.customerbase.helper;

import android.content.Context;
import android.os.Handler;

import java.util.Date;
import java.util.List;
import java.util.Map;

import in.myecash.appbase.entities.MyCashback;
import in.myecash.appbase.utilities.TxnReportsHelper;
import in.myecash.common.database.CustomerOps;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.entities.CustomerStats;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.RetainedFragment;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyRetainedFragment extends RetainedFragment {
    private static final String TAG = "CustApp-MyRetainedFragment";

    // Requests that this fragment executes in backend
    public static final int REQUEST_LOGIN = 0;
    public static final int REQUEST_LOGOUT = 1;
    public static final int REQUEST_GENERATE_PWD = 2;
    public static final int REQUEST_CHANGE_PASSWD = 3;
    public static final int REQUEST_CHANGE_MOBILE = 4;
    public static final int REQUEST_FETCH_CB = 5;
    public static final int REQUEST_CHANGE_PIN = 6;
    public static final int REQUEST_FETCH_TXNS = 7;
    public static final int REQUEST_FETCH_TXN_FILES = 8;
    public static final int REQUEST_ENABLE_ACC = 9;
    public static final int REQUEST_FETCH_CUSTOMER_OPS = 10;

    // Threads taken care by this fragment
    private MyBackgroundProcessor<String> mBackgroundProcessor;

    public CustomerUser mCustomerUser;
    public String mUserToken;

    // Cashback Data
    public List<MyCashback> mLastFetchCashbacks;
    public Map<String, MyCashback> mCashbacks;
    public Date mCbsUpdateTime;

    // stats for the customer
    public CustomerStats stats;
    public List<CustomerOps> mLastFetchCustOps;

    // params for mobile number change operation
    public String mPinMobileChange;
    public String mNewMobileNum;
    public String mOtpMobileChange;
    public String mCardMobileChange;

    // members used by 'Txn Reports Activity' to store its state, and its fragments
    public List<String> mMissingFiles;
    // 'Txn Reports Activity' store the helper instance here in onSaveInstance
    public TxnReportsHelper mTxnReportHelper;
    public List<Transaction> mLastFetchTransactions;

    // params for enabling account
    public String mAccEnableCardNum;
    public String mAccEnablePin;
    public String mAccEnableOtp;

    // An issue is observeed in SDK 19 that when back button is pressed after onPause()
    // but before onResume(), then it was causing crash.
    // Like, pressing back immediatly after screen unlock (locked when any fragment except mobile number one was visible)
    private Boolean resumeOk;

    public void reset() {
        LogMy.d(TAG,"In reset");
        mPinMobileChange = null;
        mNewMobileNum = null;
        mOtpMobileChange = null;
        mLastFetchCashbacks = null;
    }

    public Boolean getResumeOk() {
        return resumeOk;
    }

    public void setResumeOk(Boolean resumeOk) {
        LogMy.d(TAG,"Resume Ok: "+resumeOk);
        this.resumeOk = resumeOk;
    }


    /*
     * Methods to add request for processing by background thread
     */
    public void loginCustomer(String loginId, String password) {
        mBackgroundProcessor.addLoginRequest(loginId, password);
    }
    public void logoutCustomer() {
        mBackgroundProcessor.addLogoutRequest();
    }
    public void generatePassword(String loginId, String secret1) {
        mBackgroundProcessor.addPasswordRequest(loginId, secret1);
    }
    public void changePassword(String oldPasswd, String newPasswd) {
        mBackgroundProcessor.addPasswdChangeReq(oldPasswd, newPasswd);
    }
    public void changeMobileNum() {
        mBackgroundProcessor.addChangeMobileRequest();
    }
    public void fetchCashback(Long updatedSince, Context ctxt) {
        mBackgroundProcessor.addFetchCbRequest(updatedSince, ctxt);
    }
    public void changePin(String oldPin, String newPin, String cardNum) {
        mBackgroundProcessor.addPinChangeRequest(oldPin, newPin, cardNum);
    }
    public void fetchTransactions(String whereClause) {
        mBackgroundProcessor.addFetchTxnsRequest(whereClause);
    }
    public void fetchTxnFiles(Context context) {
        mBackgroundProcessor.addFetchTxnFilesRequest(context);
    }
    public void enableAccount(String loginId, String passwd) {
        mBackgroundProcessor.addEnableRequest(loginId, passwd);
    }
    public void fetchCustomerOps() {
        mBackgroundProcessor.addCustomerOpsReq();
    }

    @Override
    protected void doOnActivityCreated() {
        mCustomerUser = CustomerUser.getInstance();
    }

    @Override
    protected BackgroundProcessor<String> getBackgroundProcessor() {
        if(mBackgroundProcessor == null) {
            LogMy.d(TAG,"Creating background thread.");
            Handler responseHandler = new Handler();
            mBackgroundProcessor = new MyBackgroundProcessor<>(responseHandler, MyRetainedFragment.this);
        }
        return mBackgroundProcessor;
    }

    @Override
    protected void doOnDestroy() {
        // nothing to do
    }
}

