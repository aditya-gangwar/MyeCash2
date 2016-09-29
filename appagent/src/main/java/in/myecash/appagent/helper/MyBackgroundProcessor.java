package in.myecash.appagent.helper;

import android.os.Handler;
import android.os.Message;

import com.backendless.exceptions.BackendlessException;

import in.myecash.appagent.backendAPI.InternalUserServices;
import in.myecash.appagent.backendAPI.InternalUserServicesNoLogin;
import in.myecash.appagent.entities.AgentUser;
import in.myecash.appbase.backendAPI.CommonServices;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;

import java.io.File;
import java.io.Serializable;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyBackgroundProcessor <T> extends BackgroundProcessor<T> {
    private final static String TAG = "MyBackgroundProcessor";

    private MyRetainedFragment mRetainedFragment;

    public MyBackgroundProcessor(Handler responseHandler, MyRetainedFragment retainedFragment) {
        super(responseHandler);
        mRetainedFragment = retainedFragment;
    }

    private class MessageLogin implements Serializable {
        public String userId;
        public String password;
        public String instanceId;
    }
    private class MessageChangePassword implements Serializable {
        public String oldPasswd;
        public String newPasswd;
    }
    private class MessageSearchMerchant implements Serializable {
        public String key;
        public boolean serachById;
    }
    private class MessageDisableMerchant implements Serializable {
        public String ticketId;
        public String reason;
        public String remarks;
    }

    /*
     * Add request methods
     */
    public void addLoginRequest(String userId, String password, String instanceId) {
        MessageLogin msg = new MessageLogin();
        msg.userId = userId;
        msg.password = password;
        msg.instanceId = instanceId;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_LOGIN, msg).sendToTarget();
    }
    public void addLogoutRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_LOGOUT, null).sendToTarget();
    }
    public void addPasswordRequest(String loginId, String secret1) {
        LogMy.d(TAG, "In addPasswordRequest");
        MessageLogin msg = new MessageLogin();
        msg.userId = loginId;
        msg.password = secret1;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_GENERATE_PWD, msg).sendToTarget();
    }
    public void addPasswdChangeReq(String oldPasswd, String newPasswd) {
        MessageChangePassword msg = new MessageChangePassword();
        msg.oldPasswd = oldPasswd;
        msg.newPasswd = newPasswd;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_PASSWD,msg).sendToTarget();
    }

    public void addRegisterMerchantReq(File file) {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_REGISTER_MERCHANT, file).sendToTarget();
    }
    public void addSearchMerchantReq(String key, boolean searchById) {
        MessageSearchMerchant msg = new MessageSearchMerchant();
        msg.key = key;
        msg.serachById = searchById;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_SEARCH_MERCHANT,msg).sendToTarget();
    }
    public void addDisableMerchantReq(String ticketId, String reason, String remarks) {
        MessageDisableMerchant msg = new MessageDisableMerchant();
        msg.ticketId = ticketId;
        msg.reason = reason;
        msg.remarks = remarks;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DISABLE_MERCHANT,msg).sendToTarget();
    }


    @Override
    protected int handleMsg(Message msg) {
        int error = ErrorCodes.NO_ERROR;
        switch(msg.what) {
            case MyRetainedFragment.REQUEST_REGISTER_MERCHANT:
                error = registerMerchant((File) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_LOGIN:
                error = loginAgent((MessageLogin) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_LOGOUT:
                error = logoutAgent();
                break;
            case MyRetainedFragment.REQUEST_GENERATE_PWD:
                error = generatePassword((MessageLogin) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_CHANGE_PASSWD:
                error = changePassword((MessageChangePassword) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_SEARCH_MERCHANT:
                error = searchMerchant((MessageSearchMerchant) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_DISABLE_MERCHANT:
                error = disableMerchant((MessageDisableMerchant) msg.obj);
                break;
        }
        return error;
    }

    private int loginAgent(MessageLogin msg) {
        LogMy.d(TAG, "In loginMerchant");
        return AgentUser.getInstance().login(msg.userId, msg.password, msg.instanceId);
    }

    private int logoutAgent() {
        return AgentUser.getInstance().logout();
    }

    private int generatePassword(MessageLogin msg) {
        try {
            InternalUserServicesNoLogin.getInstance().resetInternalUserPassword(msg.userId, msg.password);
            LogMy.d(TAG,"generatePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Agent password generate failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int changePassword(MessageChangePassword msg) {

        try {
            CommonServices.getInstance().changePassword(AgentUser.getInstance().getUser_id(), msg.oldPasswd, msg.newPasswd);
            LogMy.d(TAG,"changePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Change password failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

    private int registerMerchant(File imageFile) {
        return AgentUser.getInstance().registerMerchant(mRetainedFragment.mCurrMerchant, imageFile);
    }

    private int searchMerchant(MessageSearchMerchant data) {
        return MerchantUser.pseudoLogin(data.key);
        /*
        try {
            mRetainedFragment.mCurrMerchant = AgentUser.getInstance().searchMerchant(data.key, data.serachById);
        } catch (BackendlessException e) {
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;*/
    }

    private int disableMerchant(MessageDisableMerchant data) {
        try {
            InternalUserServices.getInstance().disableMerchant(mRetainedFragment.mCurrMerchant.getAuto_id(),
                    data.ticketId, data.reason, data.remarks);
            LogMy.d(TAG,"disableMerchant success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in disableMerchant: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }


}
