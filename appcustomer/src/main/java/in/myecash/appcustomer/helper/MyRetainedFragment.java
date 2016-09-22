package in.myecash.appcustomer.helper;

import android.os.Handler;

import java.io.File;

import in.myecash.appcustomer.entities.CustomerUser;
import in.myecash.commonbase.models.Merchants;
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

    // Threads taken care by this fragment
    private MyBackgroundProcessor<String> mBackgroundProcessor;

    public CustomerUser mCustomerUser;

    // temporary members
    //public Merchants mCurrMerchant;

    public void reset() {
        LogMy.d(TAG,"In reset");
        //mCurrMerchant = null;
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

