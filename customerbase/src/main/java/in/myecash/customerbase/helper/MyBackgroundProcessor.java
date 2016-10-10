package in.myecash.customerbase.helper;

import android.os.Handler;
import android.os.Message;

import com.backendless.exceptions.BackendlessException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.database.Cashback;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;

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
    }
    private class MessageChangePassword implements Serializable {
        public String oldPasswd;
        public String newPasswd;
    }
    private class MessageChangePin implements Serializable {
        public String oldPin;
        public String newPin;
        public String cardNum;
    }

    /*
     * Add request methods
     */
    public void addLoginRequest(String userId, String password) {
        MessageLogin msg = new MessageLogin();
        msg.userId = userId;
        msg.password = password;
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
    public void addChangeMobileRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_MOBILE,null).sendToTarget();
    }
    public void addFetchCbRequest(Long updatedSince) {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_CB,updatedSince).sendToTarget();
    }
    public void addPinChangeRequest(String oldPin, String newPin, String cardNum) {
        MessageChangePin msg = new MessageChangePin();
        msg.oldPin = oldPin;
        msg.newPin = newPin;
        msg.cardNum = cardNum;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_PIN,msg).sendToTarget();
    }


    @Override
    protected int handleMsg(Message msg) {
        int error = ErrorCodes.NO_ERROR;
        switch(msg.what) {
            case MyRetainedFragment.REQUEST_LOGIN:
                error = loginCustomer((MessageLogin) msg.obj);
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
            case MyRetainedFragment.REQUEST_CHANGE_MOBILE:
                error = changeMobileNum();
                break;
            case MyRetainedFragment.REQUEST_FETCH_CB:
                error = fetchCashbacks((Long) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_CHANGE_PIN:
                error = changePin((MessageChangePin) msg.obj);
                break;
        }
        return error;
    }

    private int loginCustomer(MessageLogin msg) {
        LogMy.d(TAG, "In loginMerchant");
        return CustomerUser.getInstance().login(msg.userId, msg.password);
    }

    private int logoutAgent() {
        return CustomerUser.getInstance().logout();
    }

    private int generatePassword(MessageLogin msg) {
        return CustomerUser.resetPassword(msg.userId, msg.password);
    }

    private int changePassword(MessageChangePassword msg) {
        return CustomerUser.getInstance().changePassword(msg.oldPasswd, msg.newPasswd);
    }

    private int changeMobileNum() {
        return CustomerUser.getInstance().changeMobileNum(mRetainedFragment.mPinMobileChange,
                mRetainedFragment.mNewMobileNum, mRetainedFragment.mOtpMobileChange);
    }

    private int fetchCashbacks(Long updatedSince) {
        mRetainedFragment.mLastFetchCashbacks = null;
        try {
            List<Cashback> cashbacks = CustomerUser.getInstance().fetchCashbacks(updatedSince);

            if(cashbacks.size() > 0) {
                mRetainedFragment.mLastFetchCashbacks = new ArrayList<>(cashbacks.size());
                for (Cashback cb :
                        cashbacks) {
                    MyCashback myCb = new MyCashback();
                    myCb.init(cb, false);
                    mRetainedFragment.mLastFetchCashbacks.add(myCb);
                }
            }
        } catch (BackendlessException e) {
            mRetainedFragment.mLastFetchCashbacks = null;
            LogMy.e(TAG, "Exception in fetchCashbacks: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int changePin(MessageChangePin msg) {
        return CustomerUser.getInstance().changePin(msg.oldPin, msg.newPin, msg.cardNum);
    }
}
