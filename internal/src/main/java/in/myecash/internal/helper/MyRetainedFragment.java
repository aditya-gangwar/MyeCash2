package in.myecash.internal.helper;

import android.os.Handler;

import in.myecash.internal.entities.AgentUser;
import in.myecash.common.MyCardForAction;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.Customers;
import in.myecash.common.database.MerchantOrders;
import in.myecash.common.database.Merchants;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.RetainedFragment;

import java.io.File;
import java.util.List;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyRetainedFragment extends RetainedFragment {
    private static final String TAG = "AgentApp-MyRetainedFragment";

    // Requests that this fragment executes in backend
    public static final int REQUEST_REGISTER_MERCHANT = 0;
    public static final int REQUEST_LOGIN = 1;
    public static final int REQUEST_LOGOUT = 3;
    public static final int REQUEST_GENERATE_PWD = 5;
    public static final int REQUEST_CHANGE_PASSWD = 7;
    public static final int REQUEST_SEARCH_MERCHANT = 8;
    public static final int REQUEST_DISABLE_MERCHANT = 9;
    public static final int REQUEST_SEARCH_CUSTOMER = 10;
    public static final int REQUEST_DISABLE_CUSTOMER = 11;
    public static final int REQUEST_LIMIT_CUST_ACC = 12;
    public static final int REQUEST_SEARCH_CARD = 13;
    public static final int REQUEST_ACTION_CARDS = 14;
    public static final int REQUEST_DISABLE_CUST_CARD = 15;
    public static final int REQUEST_SEARCH_MCHNT_ORDER = 16;
    public static final int REQUEST_FETCH_ALLOTTED_CARDS = 17;
    public static final int REQUEST_CHANGE_ORDER_STATUS = 18;
    public static final int REQUEST_DEL_DUMMY_DATA = 19;

    // Threads taken care by this fragment
    private MyBackgroundProcessor<String> mBackgroundProcessor;

    public AgentUser mAgentUser;

    // temporary members
    public Merchants mCurrMerchant;
    public Customers mCurrCustomer;
    public CustomerCards mCurrMemberCard;
    public List<CustomerCards> mLastFetchedCards;

    // members used for mchnt order search
    public String mMchntOrderId;
    public String mMchntIdForOrder;
    public List<DbConstants.MCHNT_ORDER_STATUS> mSelectedStatus;
    public List<MerchantOrders> mLastFetchMchntOrders;

    // members used for bulk actions on cards
    public List<MyCardForAction> mLastCardsForAction;
    public MerchantOrders mCurrOrder;
    public boolean cardNumFetched;

    public void reset() {
        LogMy.d(TAG,"In reset");
        mCurrMerchant = null;
        mCurrCustomer = null;
        mCurrMemberCard = null;
        mLastFetchedCards = null;
        mMchntOrderId = null;
        mMchntIdForOrder = null;
        mSelectedStatus = null;
        mLastFetchMchntOrders = null;
        mLastCardsForAction = null;
        mCurrOrder = null;
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
    public void searchCustomer(String key, boolean searchById) {
        mBackgroundProcessor.addSearchCustReq(key, searchById);
    }
    public void disableCustomer(boolean isLtdMode, String ticketId, String reason, String remarks) {
        mBackgroundProcessor.addDisableCustomerReq(isLtdMode, ticketId, reason, remarks);
    }

    public void searchMemberCard(String id) {
        mBackgroundProcessor.addCardSearchReq(id);
    }
    public void execActionForCards(String cards, String action) {
        mBackgroundProcessor.addExecActionCardsReq(cards,action);
    }
    public void disableCustCard(String ticketId, String reason, String remarks) {
        mBackgroundProcessor.addDisableCustCardReq(ticketId, reason, remarks);
    }

    public void searchMchntOrder() {
        mBackgroundProcessor.addSearchOrderReq();
    }

    public void fetchAllottedCards(String orderId) {
        mBackgroundProcessor.addFetchAllottedCardsReq(orderId);
    }

    public void changeOrderStatus(MerchantOrders updatedOrder) {
        mBackgroundProcessor.addOrderStatusChangeReq(updatedOrder);
    }

    public void delDummyData() {
        mBackgroundProcessor.addDelDummyDataReq();
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

