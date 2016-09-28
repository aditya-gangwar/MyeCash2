package in.myecash.appagent.helper;

import android.os.Handler;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.common.database.Merchants;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.RetainedFragment;

import java.io.File;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyRetainedFragment extends RetainedFragment {
    private static final String TAG = "MyRetainedFragment";

    // Requests that this fragment executes in backend
    public static final int REQUEST_REGISTER_MERCHANT = 0;
    public static final int REQUEST_LOGIN = 1;
    public static final int REQUEST_LOGOUT = 3;
    public static final int REQUEST_GENERATE_PWD = 5;
    public static final int REQUEST_CHANGE_PASSWD = 7;
    public static final int REQUEST_SEARCH_MERCHANT = 8;
    public static final int REQUEST_DISABLE_MERCHANT = 9;

    // Threads taken care by this fragment
    private MyBackgroundProcessor<String> mBackgroundProcessor;

    public AgentUser mAgentUser;

    // temporary members
    public Merchants mCurrMerchant;

    public void reset() {
        LogMy.d(TAG,"In reset");
        mCurrMerchant = null;
    }

    /*
     * Methods to add request for processing by background thread
     */
    public void loginAgent(String loginId, String password, String instanceId) {
        mBackgroundProcessor.addLoginRequest(loginId, password, instanceId);
    }
    public void logoutAgent() {
        mBackgroundProcessor.addLogoutRequest();
    }
    public void generatePassword(String loginId, String secret1) {
        mBackgroundProcessor.addPasswordRequest(loginId, secret1);
    }
    public void changePassword(String oldPasswd, String newPasswd) {
        mBackgroundProcessor.addPasswdChangeReq(oldPasswd, newPasswd);
    }

    public void registerMerchant(File displayImage) {
        mBackgroundProcessor.addRegisterMerchantReq(displayImage);
    }
    public void searchMerchant(String key, boolean searchById) {
        mBackgroundProcessor.addSearchMerchantReq(key, searchById);
    }
    public void disableMerchant(String ticketId, String reason, String remarks) {
        mBackgroundProcessor.addDisableMerchantReq(ticketId, reason, remarks);
    }

    @Override
    protected void doOnActivityCreated() {
        mAgentUser = AgentUser.getInstance();
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

