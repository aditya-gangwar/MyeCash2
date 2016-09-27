package in.myecash.customerbase.helper;

import android.os.Handler;

import java.util.Date;
import java.util.List;
import java.util.Map;

import in.myecash.commonbase.entities.MyCashback;
import in.myecash.customerbase.entities.CustomerStats;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.commonbase.utilities.BackgroundProcessor;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.RetainedFragment;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyRetainedFragment extends RetainedFragment {
    private static final String TAG = "MyRetainedFragment";

    // Requests that this fragment executes in backend
    public static final int REQUEST_LOGIN = 0;
    public static final int REQUEST_LOGOUT = 1;
    public static final int REQUEST_GENERATE_PWD = 2;
    public static final int REQUEST_CHANGE_PASSWD = 3;
    public static final int REQUEST_CHANGE_MOBILE = 4;
    public static final int REQUEST_FETCH_CB = 5;

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

    // params for mobile number change operation
    public String mVerifyParamMobileChange;
    public String mNewMobileNum;
    public String mOtpMobileChange;

    public void reset() {
        LogMy.d(TAG,"In reset");
        mVerifyParamMobileChange = null;
        mNewMobileNum = null;
        mOtpMobileChange = null;
        mLastFetchCashbacks = null;
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
    public void fetchCashback(Long updatedSince) {
        mBackgroundProcessor.addFetchCbRequest(updatedSince);
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

