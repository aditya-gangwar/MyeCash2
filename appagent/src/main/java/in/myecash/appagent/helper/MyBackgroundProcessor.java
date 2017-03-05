package in.myecash.appagent.helper;

import android.os.Handler;
import android.os.Message;

import com.backendless.exceptions.BackendlessException;

import in.myecash.appagent.backendAPI.InternalUserServices;
import in.myecash.appagent.backendAPI.InternalUserServicesNoLogin;
import in.myecash.appagent.entities.AgentUser;
import in.myecash.appbase.backendAPI.CommonServices;
import in.myecash.common.MyCardForAction;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.MerchantOrders;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.merchantbase.backendAPI.MerchantServices;
import in.myecash.merchantbase.entities.MerchantUser;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyBackgroundProcessor <T> extends BackgroundProcessor<T> {
    private final static String TAG = "AgentApp-MyBackgroundProcessor";

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
    private class MessageSearchUser implements Serializable {
        public String key;
        public boolean serachById;
    }
    private class MessageDisableUser implements Serializable {
        public boolean isLtdMode;
        public String ticketId;
        public String reason;
        public String remarks;
    }
    private class MessageActionCards implements Serializable {
        public String cards;
        public String action;
        public String allocateTo;
        public String orderId;
        public boolean getCardNumsOnly;
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
        MessageSearchUser msg = new MessageSearchUser();
        msg.key = key;
        msg.serachById = searchById;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_SEARCH_MERCHANT,msg).sendToTarget();
    }
    public void addDisableMerchantReq(String ticketId, String reason, String remarks) {
        MessageDisableUser msg = new MessageDisableUser();
        msg.ticketId = ticketId;
        msg.reason = reason;
        msg.remarks = remarks;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DISABLE_MERCHANT,msg).sendToTarget();
    }
    public void addSearchCustReq(String key, boolean searchById) {
        MessageSearchUser msg = new MessageSearchUser();
        msg.key = key;
        msg.serachById = searchById;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_SEARCH_CUSTOMER,msg).sendToTarget();
    }
    public void addDisableCustomerReq(boolean isLtdMode, String ticketId, String reason, String remarks) {
        MessageDisableUser msg = new MessageDisableUser();
        msg.isLtdMode = isLtdMode;
        msg.ticketId = ticketId;
        msg.reason = reason;
        msg.remarks = remarks;

        if(isLtdMode) {
            mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_LIMIT_CUST_ACC, msg).sendToTarget();
        } else {
            mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DISABLE_CUSTOMER, msg).sendToTarget();
        }
    }

    public void addCardSearchReq(String id) {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_SEARCH_CARD, id).sendToTarget();
    }
    public void addExecActionCardsReq(String cards, String action, String allocateTo, String orderId, boolean getCardNumsOnly) {
        MessageActionCards msg = new MessageActionCards();
        msg.cards = cards;
        msg.action = action;
        msg.allocateTo = allocateTo;
        msg.orderId = orderId;
        msg.getCardNumsOnly = getCardNumsOnly;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_ACTION_CARDS, msg).sendToTarget();
    }
    public void addDisableCustCardReq(String ticketId, String reason, String remarks) {
        MessageDisableUser msg = new MessageDisableUser();
        msg.ticketId = ticketId;
        msg.reason = reason;
        msg.remarks = remarks;

        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DISABLE_CUST_CARD, msg).sendToTarget();
    }
    public void addSearchOrderReq() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_SEARCH_MCHNT_ORDER, null).sendToTarget();
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
                error = searchMerchant((MessageSearchUser) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_DISABLE_MERCHANT:
                error = disableMerchant((MessageDisableUser) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_SEARCH_CUSTOMER:
                error = searchCustomer((MessageSearchUser) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_LIMIT_CUST_ACC:
            case MyRetainedFragment.REQUEST_DISABLE_CUSTOMER:
                error = disableCustomer((MessageDisableUser) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_SEARCH_CARD:
                error = searchMemberCard((String) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_ACTION_CARDS:
                error = actionForCards((MessageActionCards) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_DISABLE_CUST_CARD:
                error = disableCustCard((MessageDisableUser) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_SEARCH_MCHNT_ORDER:
                error = searchMchntOrder();
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
        return AgentUser.resetPassword(msg.userId, msg.password);
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

    private int searchMerchant(MessageSearchUser data) {
        return MerchantUser.pseudoLogin(data.key);
    }

    private int disableMerchant(MessageDisableUser data) {
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

    private int searchCustomer(MessageSearchUser data) {
        return CustomerUser.pseudoLogin(data.key);
    }

    private int disableCustomer(MessageDisableUser data) {
        try {
            InternalUserServices.getInstance().disableCustomer(data.isLtdMode, mRetainedFragment.mCurrCustomer.getPrivate_id(),
                    data.ticketId, data.reason, data.remarks);
            LogMy.d(TAG,"disableCustomer success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in disableCustomer: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int searchMemberCard(String id) {
        try {
            mRetainedFragment.mCurrMemberCard = null;
            mRetainedFragment.mCurrMemberCard = CommonServices.getInstance().getMemberCard(id);
            LogMy.d(TAG,"searchMemberCard success");

        } catch (BackendlessException e) {
            LogMy.e(TAG, "searchMemberCard failed: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int actionForCards(MessageActionCards data) {
        try {
            List<MyCardForAction> list = InternalUserServices.getInstance().execActionForCards(data.cards, data.action,
                    data.allocateTo, data.orderId, data.getCardNumsOnly);

            // as we want to maintain the order in which the card was scanned
            // so search by scannedCode in existing list - and update all fields filled by backend
            for (MyCardForAction card : list) {
                for (MyCardForAction oldCard : mRetainedFragment.mLastCardsForAction) {
                    if(oldCard.getScannedCode().equals(card.getScannedCode())) {
                        oldCard.setCardNum(card.getCardNum());
                        if(!data.getCardNumsOnly) {
                            oldCard.setActionStatus(card.getActionStatus());
                        }
                    }
                }
            }
            LogMy.d(TAG,"actionForCards success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in actionForCards: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int disableCustCard(MessageDisableUser data) {
        try {
            CustomerCards card = mRetainedFragment.mCurrCustomer.getMembership_card();
            InternalUserServices.getInstance().disableCustCard(mRetainedFragment.mCurrCustomer.getPrivate_id(),
                    card.getCardNum(), data.ticketId, data.reason, data.remarks);
            LogMy.d(TAG,"disableCustCard success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in disableCustCard: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int searchMchntOrder() {
        // convert to string literal CSV
        String csvStr = null;
        if(mRetainedFragment.mSelectedStatus!=null && !mRetainedFragment.mSelectedStatus.isEmpty()) {
            for (DbConstants.MCHNT_ORDER_STATUS st :
                    mRetainedFragment.mSelectedStatus) {
                if(csvStr==null) {
                    csvStr = "'"+st.name()+"'";
                } else {
                    csvStr = csvStr+",'"+st.name()+"'";
                }
            }
        }
        LogMy.d(TAG,"statusCsvStr: "+csvStr);

        try {
            mRetainedFragment.mLastFetchMchntOrders = CommonServices.getInstance().getMchntOrders(
                    mRetainedFragment.mMchntIdForOrder==null?"":mRetainedFragment.mMchntIdForOrder,
                    mRetainedFragment.mMchntOrderId==null?"":mRetainedFragment.mMchntOrderId,
                    csvStr==null?"":csvStr);

            LogMy.d(TAG,"fetchMchntOrders success: "+mRetainedFragment.mLastFetchMchntOrders.size());

            // sort by time
            Collections.sort(mRetainedFragment.mLastFetchMchntOrders, new AppCommonUtil.MchntOrderComparator());

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in fetchMchntOrders: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

}
