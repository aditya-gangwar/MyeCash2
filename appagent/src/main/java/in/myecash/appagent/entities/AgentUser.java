package in.myecash.appagent.entities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.files.BackendlessFile;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.servercode.InvocationContext;

import in.myecash.appagent.backendAPI.InternalUserServices;
import in.myecash.appagent.backendAPI.InternalUserServicesNoLogin;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Merchants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;

import java.io.File;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class AgentUser {
    private static final String TAG = "AgentApp-AgentUser";

    private BackendlessUser mAgentUser;
    public String mLastRegMerchantId;
    private String mUserToken;
    /*
     * Singleton class
     */
    private AgentUser(){
        mAgentUser = new BackendlessUser();
    }

    private static AgentUser mInstance;
    public static AgentUser getInstance() {
        if(mInstance==null) {
            mInstance = new AgentUser();
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
        return (String) mAgentUser.getProperty( "user_id" );
    }
    public int getUserType()
    {
        return (Integer) mAgentUser.getProperty( "user_type" );
    }


    /*
     * Public member methods fro business logic
     */
    public int login(String userId, String password, String instanceId) {
        LogMy.d(TAG, "In login");
        try {
            LogMy.d(TAG, "Calling setDeviceForLogin: "+userId+","+instanceId);
            InternalUserServicesNoLogin.getInstance().setDeviceForInternalUserLogin(userId, instanceId);
            LogMy.d(TAG,"setDeviceForLogin success");

            mAgentUser = Backendless.UserService.login(userId, password, false);
            int userType = (Integer)mAgentUser.getProperty("user_type");
            if(  userType != DbConstants.USER_TYPE_AGENT &&
                    userType != DbConstants.USER_TYPE_CC &&
                    userType != DbConstants.USER_TYPE_CCNT) {
                // wrong user type
                LogMy.e(TAG,"Invalid usertype in agent app: "+userType+", "+userId);
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
        mAgentUser = null;
        return errorCode;
    }


    /*
     * Public member methods for merchant operations
     */
    /*public Merchants searchMerchant(String key, boolean searchById) {
        BackendlessDataQuery query = new BackendlessDataQuery();
        if(searchById) {
            query.setWhereClause("auto_id = '"+key+"'");
        } else {
            query.setWhereClause("mobile_num = '"+key+"'");
        }

        BackendlessCollection<Merchants> user = Backendless.Data.of( Merchants.class ).find(query);
        if( user.getTotalObjects() == 0) {
            String errorMsg = "No Merchant found: "+key;
            throw new BackendlessException(String.valueOf(ErrorCodes.NO_SUCH_USER), errorMsg);
        } else {
            return user.getData().get(0);
        }
    }*/

    public int registerMerchant(Merchants merchant, File imgFile) {
        // register merchant
        LogMy.d(TAG, "In registerMerchant");
        try {
            String url = uploadImageSync(imgFile, CommonConstants.MERCHANT_DISPLAY_IMAGES_DIR);
            if(url != null) {
                merchant.setDisplayImage(imgFile.getName());
                mLastRegMerchantId = InternalUserServices.getInstance().registerMerchant(merchant);
                LogMy.d(TAG, "Merchant registration success: " + merchant.getAuto_id());

            } else {
                return ErrorCodes.FILE_UPLOAD_FAILED;
            }
        } catch(BackendlessException e) {
            LogMy.e(TAG, "Merchant registration failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public String getUserToken() {
        return mUserToken;
    }

    /*
     * Private helper methods
     */
    private String uploadImageSync(File imgFile, String remoteDir) {
        // upload file
        try {
            //LogMy.d(TAG,"In uploadImageSync: "+mUserToken+","+HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY));
            //HeadersManager.getInstance().addHeader( HeadersManager.HeadersEnum.USER_TOKEN_KEY, mUserToken );

            for( String key : HeadersManager.getInstance().getHeaders().keySet() ) {
                LogMy.d(TAG, "In uploadImageSync: " + key + "," + HeadersManager.getInstance().getHeaders().get(key));
            }

            BackendlessFile file = Backendless.Files.upload(imgFile, remoteDir, true);
            LogMy.d(TAG, "Image uploaded successfully at :" + file.getFileURL());
            return file.getFileURL();

        } catch(Exception e) {
            LogMy.e(TAG, "Image file upload failed: " + e.toString());
        }
        return null;
    }
}
