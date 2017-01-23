package in.myecash.customerbase.entities;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.backendless.persistence.local.UserTokenStorageFactory;
import com.crashlytics.android.Crashlytics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import in.myecash.appbase.backendAPI.CommonServices;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.common.DateUtil;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.CustomerOps;
import in.myecash.common.database.Customers;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.backendAPI.CustomerServices;
import in.myecash.customerbase.backendAPI.CustomerServicesNoLogin;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class CustomerUser {
    private static final String TAG = "CustApp-CustomerUser";

    private static CustomerUser mInstance;

    private Customers mCustomer;
    private boolean mPseudoLoggedIn;
    private String mUserToken;
    /*
     * Singleton class
     */
    private CustomerUser(){
        //mCustomerUser = new BackendlessUser();
    }

    public static CustomerUser getInstance() {
        /*if(mInstance==null) {
            mInstance = new CustomerUser();
        }*/
        return mInstance;
    }
    private static void createInstance() {
        if (mInstance == null) {
            LogMy.d(TAG, "Creating CustomerUser instance");
            mInstance = new CustomerUser();
            mInstance.mPseudoLoggedIn = false;
        }
    }

    /*
     * Static public methods
     */
    public static void reset() {
        if(mInstance!=null) {
            mInstance.mCustomer = null;
            //mInstance.mBackendlessUser = null;
            mInstance = null;
        }
    }


    /*
     * Methods to restore loginId / password
     */
    public static int resetPassword(String custMobile, String secret) {
        try {
            CustomerServicesNoLogin.getInstance().resetCustomerPassword(custMobile, secret);
            LogMy.d(TAG,"generatePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Customer password generate failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    /*
     * Login-Logout methods
     * CustomerUser singleton instance is created on successfull login
     * and destroyed on logout / explicit reset.
     */
    /*public static void isValidLogin(AsyncCallback<Boolean> responder) {
        //return Backendless.UserService.isValidLogin();
        Backendless.UserService.isValidLogin( responder );
    }*/

    public static int tryAutoLogin() {
        LogMy.d(TAG, "In tryAutoLogin");
        try {
            if(!Backendless.UserService.isValidLogin()) {
                return ErrorCodes.NOT_LOGGED_IN;
            }
            BackendlessUser user = null;
            String currentUserObjectId = UserIdStorageFactory.instance().getStorage().get();
            LogMy.d(TAG, "currentUserObjectId: "+currentUserObjectId);

            if(currentUserObjectId!=null && !currentUserObjectId.isEmpty()) {
                user = getCustUserById(currentUserObjectId);
                if(user==null) {
                    return ErrorCodes.GENERAL_ERROR;
                }
                Backendless.UserService.setCurrentUser(user);
            }
            int retStatus = loadOnLogin(user);
            if( retStatus!= ErrorCodes.NO_ERROR) {
                return retStatus;
            }
        } catch (BackendlessException e) {
            LogMy.d(TAG,"Auto Login failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public static int login(String userId, String password) {
        LogMy.d(TAG, "In login");
        try {

            BackendlessUser user = Backendless.UserService.login(userId, password, true);
            LogMy.d(TAG, "Customer Login Success: " + userId);

            int retStatus = loadOnLogin(user);
            if( retStatus!= ErrorCodes.NO_ERROR) {
                logout();
                return retStatus;
            }

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Login failed: "+e.toString());
            logout();
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    // This method doesn't do any login, but downloads corresponding customer Backendless user and object
    public static int pseudoLogin(String customerMob) {
        LogMy.d(TAG, "In pseudoLogin: "+customerMob);

        try {
            // create instance of CustomerUser class
            createInstance();
            mInstance.loadCustomer(customerMob);
            mInstance.mPseudoLoggedIn = true;

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception while pseudo login: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public static int logout() {
        LogMy.d(TAG, "In logout");
        if(mInstance!=null && !mInstance.mPseudoLoggedIn) {
            try {
                Backendless.UserService.logout();
                //LogMy.d(TAG, "Logout Success: " + mInstance.mCustomer.getMobile_num());
            } catch (BackendlessException e) {
                LogMy.e(TAG, "Logout failed: " + e.toString());
                return AppCommonUtil.getLocalErrorCode(e);
            }
        }
        // reset all
        reset();
        return ErrorCodes.NO_ERROR;
    }

    public static int enableAccount(String userId, String passwd, String rcvdOtp, String cardNum, String pin) {
        LogMy.d(TAG, "In enableAccount");
        try {
            CustomerServicesNoLogin.getInstance().enableCustAccount(userId, passwd, rcvdOtp, cardNum, pin);
            LogMy.d(TAG,"enableAccount success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"enableAccount failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public List<CustomerOps> fetchCustomerOps() throws BackendlessException {
        LogMy.d(TAG, "In fetchCustomerOps");
        isLoginValid();

        return CustomerServices.getInstance().getCustomerOps(mCustomer.getPrivate_id());
    }

    /*
     * Methods to change profile / settings
     */
    public int changePassword(String oldPasswd, String newPasswd) {
        LogMy.d(TAG, "In changePassword: ");
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        try {
            isLoginValid();
            CommonServices.getInstance().changePassword(mCustomer.getMobile_num(), oldPasswd, newPasswd);
            LogMy.d(TAG,"changePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Change password failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

    public int changeMobileNum(String cardNum, String pin, String newMobile, String otp) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        try {
            isLoginValid();
            CommonServices.getInstance().execCustomerOp(DbConstants.OP_CHANGE_MOBILE, mCustomer.getMobile_num(),
                    cardNum, otp, pin, newMobile);
            LogMy.d(TAG,"changeMobileNum success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"changeMobileNum failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

    public int changePin(String oldPin, String newPin, String cardNum) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        try {
            isLoginValid();
            if(oldPin==null || newPin==null) {
                // PIN reset scenario
                CommonServices.getInstance().execCustomerOp(DbConstants.OP_RESET_PIN, mCustomer.getMobile_num(),
                        cardNum, "", oldPin, newPin);
            } else {
                // PIN change scenario
                CommonServices.getInstance().execCustomerOp(DbConstants.OP_CHANGE_PIN, mCustomer.getMobile_num(),
                        mCustomer.getMembership_card().getCardNum(), "", oldPin, newPin);
            }

        } catch (BackendlessException e) {
            LogMy.e(TAG,"changePin failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

    public List<Cashback> fetchCashbacks(Long updatedSince) throws BackendlessException {
        LogMy.d(TAG, "In fetchCashback");
        isLoginValid();
        return CustomerServices.getInstance().getCashbacks(mCustomer.getPrivate_id(), updatedSince);
    }

    public List<Transaction> fetchTxns(String whereClause) throws BackendlessException {
        LogMy.d(TAG, "In fetchTxns");
        isLoginValid();
        return CustomerServices.getInstance().getTransactions(mCustomer.getPrivate_id(), whereClause);
    }


    /*
     * Getter / Setter
     */
    public String getUserToken() {
        return mUserToken;
    }

    public Customers getCustomer()
    {
        return mCustomer;
    }

    public boolean isPseudoLoggedIn() {
        return mPseudoLoggedIn;
    }

    /*
     * Private helper methods
     */
    private static BackendlessUser getCustUserById(String objectId) {
        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("customer");
        relationProps.add("customer.membership_card");
        return Backendless.Data.of( BackendlessUser.class ).findById( objectId, relationProps );
    }

    private static int loadOnLogin(BackendlessUser user) {
        LogMy.d(TAG, "In loadOnLogin");
        try {
            String userId = (String) user.getProperty("user_id");
            int userType = (Integer)user.getProperty("user_type");
            if( userType != DbConstants.USER_TYPE_CUSTOMER) {
                // wrong user type
                LogMy.e(TAG,"Invalid usertype in customer app: "+userType+", "+userId);
                return ErrorCodes.USER_WRONG_ID_PASSWD;
            }

            // create instance of CustomerUser class
            createInstance();
            mInstance.mCustomer = (Customers) user.getProperty("customer");
            mInstance.initWithCustObject();

            // load all child objects
            //mInstance.loadCustomer(userId);
            LogMy.d(TAG, "Customer Load Success: " + mInstance.mCustomer.getMobile_num());

            // Store user token
            mInstance.mUserToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
            if(mInstance.mUserToken == null || mInstance.mUserToken.isEmpty()) {
                logout();
                return ErrorCodes.GENERAL_ERROR;
            }

            return loadGlobalSettings();

        } catch (BackendlessException e) {
            LogMy.e(TAG,"loadOnLogin failed: "+e.toString());
            throw e;
        }
        //return ErrorCodes.NO_ERROR;
    }

    private static int loadGlobalSettings() {
        try {
            MyGlobalSettings.initSync(MyGlobalSettings.RunMode.appCustomer);
            return checkServiceTime();

        } catch (Exception e) {
            LogMy.e(TAG,"Failed to fetch global settings: "+e.toString());
            AppAlarms.handleException(e);
            if(e instanceof BackendlessException) {
                return AppCommonUtil.getLocalErrorCode((BackendlessException) e);
            }
            return ErrorCodes.GENERAL_ERROR;
        }
    }

    private static int checkServiceTime() {
        // Check for daily downtime
        int startHour = MyGlobalSettings.getDailyDownStartHour();
        int endHour = MyGlobalSettings.getDailyDownEndHour();
        if(endHour > startHour) {
            int currHour = (new DateUtil()).getHourOfDay();
            if(currHour >= startHour && currHour < endHour) {
                return ErrorCodes.UNDER_DAILY_DOWNTIME;
            }
        }
        // Check maintenance window time
        Date disabledUntil = MyGlobalSettings.getServiceDisabledUntil();
        if(disabledUntil == null || System.currentTimeMillis() > disabledUntil.getTime()) {
            return ErrorCodes.NO_ERROR;
        } else {
            return ErrorCodes.SERVICE_GLOBAL_DISABLED;
        }
    }

    private void isLoginValid() {
        // Not working properly - so commenting it out
        /*String userToken = UserTokenStorageFactory.instance().getStorage().get();
        if(userToken==null || userToken.isEmpty()) {
            LogMy.e(TAG,"User token is null. Auto logout scenario");
            throw new BackendlessException(String.valueOf(ErrorCodes.NOT_LOGGED_IN), "");
        }*/
    }

    private void loadCustomer(String mobileNum) {
        mCustomer = CommonServices.getInstance().getCustomer(mobileNum);
        initWithCustObject();
    }

    private void initWithCustObject() {
        // map cashback and transaction table
        //Backendless.Data.mapTableToClass(mCustomer.getCashback_table(), Cashback.class);

        String[] csvFields = mCustomer.getTxn_tables().split(CommonConstants.CSV_DELIMETER, -1);
        for (String str : csvFields) {
            Backendless.Data.mapTableToClass(str, Transaction.class);
        }

        csvFields = mCustomer.getCashback_table().split(CommonConstants.CSV_DELIMETER, -1);
        for (String str : csvFields) {
            Backendless.Data.mapTableToClass(str, Cashback.class);
        }
        // Set user id for crashlytics
        Crashlytics.setUserIdentifier(mCustomer.getPrivate_id());
    }
}
