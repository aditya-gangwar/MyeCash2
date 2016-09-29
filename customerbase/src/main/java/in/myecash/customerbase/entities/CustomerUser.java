package in.myecash.customerbase.entities;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.crashlytics.android.Crashlytics;

import java.util.List;

import in.myecash.appbase.backendAPI.CommonServices;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.Customers;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.customerbase.backendAPI.CustomerServices;
import in.myecash.customerbase.backendAPI.CustomerServicesNoLogin;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class CustomerUser {
    private static final String TAG = "CustomerUser";

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
    public int login(String userId, String password) {
        LogMy.d(TAG, "In login");
        try {
            BackendlessUser user = Backendless.UserService.login(userId, password, false);
            int userType = (Integer)user.getProperty("user_type");
            if( userType != DbConstants.USER_TYPE_CUSTOMER) {
                // wrong user type
                LogMy.e(TAG,"Invalid usertype in customer app: "+userType+", "+userId);
                logout();
                return ErrorCodes.USER_WRONG_ID_PASSWD;
            }

            // create instance of CustomerUser class
            createInstance();
            // load all child objects
            mInstance.loadCustomer(userId);

            // Store user token
            mInstance.mUserToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
            if(mInstance.mUserToken == null || mInstance.mUserToken.isEmpty()) {
                logout();
                return ErrorCodes.GENERAL_ERROR;
            }
            LogMy.d(TAG, "Customer Login Success: " + mInstance.mCustomer.getMobile_num());

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

    public int logout() {
        LogMy.d(TAG, "In logout");
        if(!mInstance.mPseudoLoggedIn) {
            try {
                Backendless.UserService.logout();
                LogMy.d(TAG, "Logout Success: " + mInstance.mCustomer.getMobile_num());
            } catch (BackendlessException e) {
                LogMy.e(TAG, "Logout failed: " + e.toString());
                return AppCommonUtil.getLocalErrorCode(e);
            }
        }
        // reset all
        reset();
        return ErrorCodes.NO_ERROR;
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
            CommonServices.getInstance().changePassword(mCustomer.getMobile_num(), oldPasswd, newPasswd);
            LogMy.d(TAG,"changePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Change password failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

    public int changeMobileNum(String verifyParam, String newMobile, String otp) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        int returnCode = ErrorCodes.NO_ERROR;
        try {
            mCustomer = CustomerServices.getInstance().changeMobile(verifyParam, newMobile, otp);
            LogMy.d(TAG,"changeMobileNum success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"changeMobileNum failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return returnCode;
    }

    public int changePin(String oldPin, String newPin, String cardNum) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        int returnCode = ErrorCodes.NO_ERROR;
        try {
            CustomerServices.getInstance().changePin(oldPin, newPin, cardNum);
            LogMy.d(TAG,"changePin success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"changePin failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return returnCode;
    }

    public List<Cashback> fetchCashbacks(Long updatedSince) throws BackendlessException {
        LogMy.d(TAG, "In fetchCashback");
        return CustomerServices.getInstance().getCashbacks(updatedSince);
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
    private void loadCustomer(String mobileNum) {

        mCustomer = CommonServices.getInstance().getCustomer(mobileNum);

        // map cashback and transaction table
        //Backendless.Data.mapTableToClass(mMerchant.getCashback_table(), Cashback.class);
        //Backendless.Data.mapTableToClass(mMerchant.getTxn_table(), Transaction.class);

        // Set user id for crashlytics
        Crashlytics.setUserIdentifier(mCustomer.getPrivate_id());
    }

}
