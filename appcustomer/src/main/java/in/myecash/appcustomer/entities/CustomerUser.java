package in.myecash.appcustomer.entities;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class CustomerUser {
    private static final String TAG = "CustomerUser";

    private BackendlessUser mCustomerUser;
    private String mUserToken;
    /*
     * Singleton class
     */
    private CustomerUser(){
        mCustomerUser = new BackendlessUser();
    }

    private static CustomerUser mInstance;
    public static CustomerUser getInstance() {
        if(mInstance==null) {
            mInstance = new CustomerUser();
        }
        return mInstance;
    }
    public static void reset() {
        mInstance = null;
    }


    /*
     * Public member get/set methods
     */
    public String getUser_id()
    {
        return (String) mCustomerUser.getProperty( "user_id" );
    }
    public int getUserType()
    {
        return (Integer) mCustomerUser.getProperty( "user_type" );
    }


    /*
     * Public member methods fro business logic
     */
    public int login(String userId, String password) {
        LogMy.d(TAG, "In login");
        try {
            mCustomerUser = Backendless.UserService.login(userId, password, false);
            int userType = (Integer)mCustomerUser.getProperty("user_type");
            if( userType != DbConstants.USER_TYPE_CUSTOMER) {
                // wrong user type
                LogMy.e(TAG,"Invalid usertype in customer app: "+userType+", "+userId);
                logout();
                return ErrorCodes.USER_WRONG_ID_PASSWD;
            }

            // Store user token
            mInstance.mUserToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
            if(mInstance.mUserToken == null || mInstance.mUserToken.isEmpty()) {
                logout();
                return ErrorCodes.GENERAL_ERROR;
            }
            LogMy.d(TAG, "Login Success: " + getUser_id());

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Login failed: "+e.toString());
            logout();
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public int logout() {
        LogMy.d(TAG, "In logout");
        int errorCode = ErrorCodes.NO_ERROR;

        try {
            Backendless.UserService.logout();
            LogMy.d(TAG, "Logout Success: " + getUser_id());
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Logout failed: "+e.toString());
            errorCode = ErrorCodes.GENERAL_ERROR;
        }

        // reset all
        mCustomerUser = null;
        return errorCode;
    }

    public String getUserToken() {
        return mUserToken;
    }

    /*
     * Private helper methods
     */
}
