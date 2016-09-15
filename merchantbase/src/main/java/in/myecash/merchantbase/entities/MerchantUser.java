package in.myecash.merchantbase.entities;

/**
 * Created by adgangwa on 19-02-2016.
 */

import android.graphics.Bitmap;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.files.BackendlessFile;
import com.crashlytics.android.Crashlytics;
import in.myecash.commonbase.backendAPI.CommonServices;
import in.myecash.commonbase.constants.BackendResponseCodes;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.entities.MyGlobalSettings;
import in.myecash.commonbase.models.Cashback;
import in.myecash.commonbase.models.MerchantDevice;
import in.myecash.commonbase.models.MerchantStats;
import in.myecash.commonbase.models.Merchants;
import in.myecash.commonbase.models.Transaction;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.backendAPI.MerchantServices;
import in.myecash.merchantbase.backendAPI.MerchantServicesNoLogin;

import java.io.File;
import java.util.Date;
import java.util.List;

public class MerchantUser
{
    private static final String TAG = "MerchantUser";

    private static MerchantUser mInstance;

    // instance members
    private Bitmap mDisplayImage;
    private String mUserToken;
    //private String mDeviceInfo;
    //private String mDeviceId;

    //private BackendlessUser mBackendlessUser;
    private Merchants mMerchant;
    private boolean mPseudoLoggedIn;

    // fields that can be changed during lifecycle
    private String mNewCbRate;
    private int mNewIsAddClEnabled;
    private String mNewEmail;

    /*
     * Singleton class
     */
    private MerchantUser(){
        //mBackendlessUser = new BackendlessUser();
        //mMerchant = new Merchants();
    }

    public static MerchantUser getInstance() {
        /*
        if(mInstance==null) {
            mInstance = new MerchantUser();
            mInstance.mNewIsAddClEnabled = CommonConstants.BOOLEAN_VALUE_INVALID;
            mInstance.mPseudoLoggedIn = false;
        }*/
        return mInstance;
    }

    private static void createInstance() {
        if(mInstance==null) {
            LogMy.d(TAG, "Creating MerchantUser instance");
            mInstance = new MerchantUser();
            mInstance.mNewIsAddClEnabled = CommonConstants.BOOLEAN_VALUE_INVALID;
            mInstance.mPseudoLoggedIn = false;
        }
    }

    /*
     * Static public methods
     */
    public static void reset() {
        if(mInstance!=null) {
            mInstance.mMerchant = null;
            //mInstance.mBackendlessUser = null;
            mInstance = null;
        }
    }

    /*
     * Methods to restore loginId / password
     */
    public static int resetPassword(String brandName, String userId, String deviceId) {
        try {
            MerchantServicesNoLogin.getInstance().resetMerchantPwd(userId, deviceId, brandName);
            LogMy.d(TAG,"generatePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Merchant password generate failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public static int forgotId(String mobileNum, String deviceId) {
        try {
            MerchantServicesNoLogin.getInstance().sendMerchantId(mobileNum, deviceId);
            LogMy.d(TAG,"forgotId success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Merchant send id failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    /*
     * Login-Logout methods
     * MerchantUser singleton instance is created on successfull login
     * and destroyed on logout / explicit reset.
     */
    public static int login(String userId, String password, String deviceId, String otp) {
        LogMy.d(TAG, "In login");
        try {
            // create instance of MerchantUser class
            createInstance();

            String deviceInfo = deviceId+","
                    + AppCommonUtil.getDeviceManufacturer()+","
                    + AppCommonUtil.getDeviceModel()+","
                    + AppCommonUtil.getAndroidVersion();

            LogMy.d(TAG, "Calling setDeviceForLogin: "+userId+", "+deviceInfo);
            MerchantServicesNoLogin.getInstance().setDeviceForLogin(userId, deviceInfo, otp);
            LogMy.d(TAG,"setDeviceForLogin success");

            //mInstance.mBackendlessUser = Backendless.UserService.login(userId, password, false);
            BackendlessUser user = Backendless.UserService.login(userId, password, false);
            if(  DbConstants.USER_TYPE_MERCHANT != (Integer)user.getProperty("user_type") ) {
                // wrong user type
                logoutSync();
                return ErrorCodes.USER_WRONG_ID_PASSWD;
            }

            // load all child objects
            //mInstance.loadMerchant(user);
            mInstance.loadMerchant(userId);

            // Store user token
            mInstance.mUserToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
            if(mInstance.mUserToken == null || mInstance.mUserToken.isEmpty()) {
                logoutSync();
                return ErrorCodes.GENERAL_ERROR;
            }
            LogMy.d(TAG, "Login Success: " + mInstance.mMerchant.getAuto_id());

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Login failed: "+e.toString());
            logoutSync();
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    // This method doesn't do any login, but downloads corresponding merchant Backendless user and object
    public static int pseudoLogin(String mchntIdorMobile) {
        LogMy.d(TAG, "In pseudoLogin: "+mchntIdorMobile);

        try {
            // create instance of MerchantUser class
            createInstance();

            mInstance.loadMerchant(mchntIdorMobile);
            mInstance.mPseudoLoggedIn = true;

            /*
            // Get backendless user for this merchant
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause("user_id = '" + mchntIdorMobile + "'");

            QueryOptions queryOptions = new QueryOptions();
            queryOptions.addRelated("merchant");
            queryOptions.addRelated("merchant.trusted_devices");
            queryOptions.addRelated("merchant.address");
            queryOptions.addRelated("merchant.address.city");
            queryOptions.addRelated("merchant.buss_category");
            query.setQueryOptions(queryOptions);

            BackendlessCollection<BackendlessUser> user = Backendless.Data.of(BackendlessUser.class).find(query);
            if (user.getTotalObjects() == 0) {
                reset();
                return ErrorCodes.USER_NOT_REGISTERED;
            } else {
                LogMy.d(TAG,"Fetched Merchant object successfully");
                //mInstance.mBackendlessUser = user.getData().get(0);
                //mInstance.mMerchant = (Merchants)mInstance.mBackendlessUser.getProperty("merchant");
                mInstance.mMerchant = CommonServices.getInstance().getMerchant(mchntIdorMobile);

                // map cashback and transaction table
                Backendless.Data.mapTableToClass(mInstance.mMerchant.getCashback_table(), Cashback.class);
                Backendless.Data.mapTableToClass(mInstance.mMerchant.getTxn_table(), Transaction.class);

                // Set user id for crashlytics
                Crashlytics.setUserIdentifier(mInstance.mMerchant.getAuto_id());

                mInstance.mPseudoLoggedIn = true;
            }*/
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception while pseudo login: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public static int logoutSync() {
        LogMy.d(TAG, "In logoutSync");

        if(!mInstance.mPseudoLoggedIn) {
            try {
                Backendless.UserService.logout();
                LogMy.d(TAG, "Logout Success: " + mInstance.mMerchant.getAuto_id());
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
            return ErrorCodes.NO_PERMISSIONS;
        }

        try {
            CommonServices.getInstance().changePassword(mMerchant.getAuto_id(), oldPasswd, newPasswd);
            LogMy.d(TAG,"changePassword success");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Change password failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        return ErrorCodes.NO_ERROR;
    }

    public int updateSettings() {
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }

        try {
            // set new value = old value - for non changed params
            if(mNewCbRate==null) {
                mNewCbRate = mMerchant.getCb_rate();
            }
            boolean newAddClEnabled = mMerchant.getCl_add_enable();
            if(mNewIsAddClEnabled != CommonConstants.BOOLEAN_VALUE_INVALID) {
                newAddClEnabled = (mNewIsAddClEnabled==CommonConstants.BOOLEAN_VALUE_TRUE);
            }
            if(mNewEmail==null) {
                mNewEmail = mMerchant.getEmail();
            }

            mMerchant = MerchantServices.getInstance().updateSettings(mNewCbRate, newAddClEnabled, mNewEmail);
            LogMy.d(TAG,"updateSettings success");

            // reload merchant object
            /*
            try {
                loadMerchant();
            } catch(BackendlessException e) {
                LogMy.e(TAG,"Failed to load merchant object, after settings update");
                // manually update locally
                mMerchant.setCb_rate(mNewCbRate);
                mMerchant.setCl_add_enable(newAddClEnabled);
                mMerchant.setEmail(mNewEmail);
                throw e;
            }*/
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Merchant settings update failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        // reset new setting variables - for both error and success scenarios
        mNewCbRate = null;
        mNewIsAddClEnabled = CommonConstants.BOOLEAN_VALUE_INVALID;
        mNewEmail = null;

        return ErrorCodes.NO_ERROR;
    }

    public int changeMobileNum(String currMobile, String newMobile, String otp) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }

        int returnCode = ErrorCodes.NO_ERROR;
        try {
            mMerchant = MerchantServices.getInstance().changeMobile(currMobile, newMobile, otp);
            LogMy.d(TAG,"changeMobileNum success");

            /*
            try {
                loadMerchant();
            } catch(BackendlessException e) {
                LogMy.e(TAG,"Failed to load merchant object, after settings update");
                // manually update locally
                setMobileNum(newMobile);
                throw e;
            }*/

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Merchant settings update failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        // reset new setting variables - for both error and success scenarios
        mNewCbRate = null;
        mNewIsAddClEnabled = CommonConstants.BOOLEAN_VALUE_INVALID;
        mNewEmail = null;

        return returnCode;
    }


    /*
     * Methods for customer specific tasks - allowed to merchants
     */
    public int executeCustOp(CustomerOps custOp) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }

        try
        {
            MerchantServices.getInstance().execCustomerOp(custOp.getOp_code(),custOp.getMobile_num(),custOp.getQr_card(),
                    custOp.getOtp(),custOp.getPin(),custOp.getExtra_op_params());
        }
        catch( BackendlessException e )
        {
            LogMy.e(TAG, "exec customer op failed: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public Cashback registerCustomer(String mobileNum, String name, String qrCode) {
        if(mPseudoLoggedIn) {
            // intentionally using 'Backend' error code - as calling fx. will try to convert
            throw new BackendlessException(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED, "");
        }
        return MerchantServices.getInstance().registerCustomer(mobileNum, name, qrCode);
    }

    /*
     * Methods for DB fetches
     */
    public MerchantStats fetchStats() throws BackendlessException {
        return MerchantServices.getInstance().getMerchantStats(mMerchant.getAuto_id());
    }

    public Cashback fetchCashback(String custId) {
        LogMy.d(TAG, "In fetchCashback");

        return MerchantServices.getInstance().getCashback(
                mMerchant.getAuto_id(),
                mMerchant.getCashback_table(),
                custId,
                mMerchant.getDebugLogs());
    }

    /*
     * Methods for DB uploads / commits
     */
    public int commitTxn(MyTransaction txn, String pin) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }
        return txn.commit(pin);
    }

    public String uploadTxnImgFile(File file) {
        if(mPseudoLoggedIn) {
            return null;
        }
        return uploadImageSync(file,AppCommonUtil.getTxnImgDir(mMerchant.getAuto_id()));
    }

    /*
     * Methods for other DB actions
     */
    public int archiveTxns() {
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }

        try {
            MerchantServices.getInstance().archiveTxns();
            LogMy.d(TAG,"archiveTxns success");
        } catch (BackendlessException e) {
            LogMy.e(TAG, "archiveTxns failed: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public int deleteTrustedDevice(int index) {
        LogMy.d(TAG, "In deleteTrustedDevice: " + index);
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }
        try {
            mMerchant = MerchantServices.getInstance().deleteTrustedDevice(mMerchant.getTrusted_devices().get(index).getDevice_id());
            LogMy.d(TAG, "Device delete success: " + mMerchant.getAuto_id());
        } catch(BackendlessException e) {
            LogMy.e(TAG, "Device delete failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    /*
    public int deleteTrustedDevice(int index) {
        LogMy.d(TAG, "In deleteTrustedDevice: " + index);
        if(mPseudoLoggedIn) {
            return ErrorCodes.NO_PERMISSIONS;
        }
        // One step deletion - as suggested in backendless docs was not working
        // so doing as below
        try {
            Backendless.Persistence.of( MerchantDevice.class ).remove(mMerchant.getTrusted_devices().get(index));
            LogMy.d(TAG, "Device delete success: " + mMerchant.getAuto_id());
            int status = loadTrustedDevices();
            if(status != ErrorCodes.NO_ERROR) {
                // remove manually
                LogMy.w(TAG,"Trusted device upload failed, updating manually");
                mMerchant.getTrusted_devices().remove(index);
            }
            return ErrorCodes.NO_ERROR;
        } catch(BackendlessException e) {
            LogMy.e(TAG, "Device delete failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
    }*/

    /*
     * Getter / Setter
     */
    public int getClDebitLimitForPin() {
        if(mMerchant.getCl_debit_limit_for_pin() == null) {
            return MyGlobalSettings.getAccDebitPinLimit();
        }
        return mMerchant.getCl_debit_limit_for_pin();
    }

    public int getCbDebitLimitForPin() {
        if(mMerchant.getCb_debit_limit_for_pin() == null) {
            return MyGlobalSettings.getCbDebitPinLimit();
        }
        return mMerchant.getCb_debit_limit_for_pin();
    }

    public int getClCreditLimitForPin() {
        if(mMerchant.getCl_credit_limit_for_pin() == null) {
            return MyGlobalSettings.getAccAddPinLimit();
        }
        return mMerchant.getCl_credit_limit_for_pin();
    }

    public String getUserToken() {
        return mUserToken;
    }

    public void setMobileNum(String mobileNum) {
        mMerchant.setMobile_num(mobileNum);
    }

    public String getMerchantId() {
        return mMerchant.getAuto_id();
    }

    public String getMerchantName() {
        return mMerchant.getName();
    }

    public Bitmap getDisplayImage() {
        return mDisplayImage;
    }
    public void setDisplayImage(Bitmap displayImage) {
        mDisplayImage = displayImage;
    }

    /*
    public String getDeviceInfo() {
        return mDeviceInfo;
    }
    public void setDeviceInfo(String mDeviceInfo) {
        this.mDeviceInfo = mDeviceInfo;
    }

    public String getDeviceId() {
        return mDeviceId;
    }
    public void setDeviceId(String mDeviceId) {
        this.mDeviceId = mDeviceId;
    }*/

    public List<MerchantDevice> getTrustedDeviceList() {
        return mMerchant.getTrusted_devices();
    }

    public String getCashbackTableName() {
        return mMerchant.getCashback_table();
    }

    /*
    public String getUser_id()
    {
        return (String) mBackendlessUser.getProperty( "user_id" );
    }

    public void setUser_id( String user_id )
    {
        mBackendlessUser.setProperty("user_id", user_id);
    }*/

    public Merchants getMerchant()
    {
        return mMerchant;
    }

    // Public methods for changing merchant properties
    public void setNewEmail(String newEmail) {
        mNewEmail = newEmail;
    }

    public void setNewIsAddClEnabled(boolean newIsAddClEnabled) {
        mNewIsAddClEnabled = newIsAddClEnabled ? CommonConstants.BOOLEAN_VALUE_TRUE : CommonConstants.BOOLEAN_VALUE_FALSE;
    }

    public void setNewCbRate(String newCbRate) {
        mNewCbRate = newCbRate;
    }


    /*
     * Private helper functions
     */
    private String uploadImageSync(File imgFile, String remoteDir) {
        // upload file
        try {
            BackendlessFile file = Backendless.Files.upload(imgFile, remoteDir, true);
            LogMy.d(TAG, "Image uploaded successfully at :" + file.getFileURL());
            return file.getFileURL();
        } catch(Exception e) {
            LogMy.e(TAG, "Image file upload failed: " + e.toString());
        }
        return null;
    }

    /*
    private int loadTrustedDevices() throws BackendlessException {
        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("trusted_devices");
        try {
            Backendless.Data.of( Merchants.class ).loadRelations(mMerchant, relationProps);
            mBackendlessUser.setProperty("merchant", mMerchant);
        } catch (BackendlessException e) {
            LogMy.e(TAG,"loadTrustedDevices failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }*/

    private void loadMerchant(String idOrMobileNum) {

        mMerchant = CommonServices.getInstance().getMerchant(idOrMobileNum);

        // map cashback and transaction table
        Backendless.Data.mapTableToClass(mMerchant.getCashback_table(), Cashback.class);
        Backendless.Data.mapTableToClass(mMerchant.getTxn_table(), Transaction.class);

        // Set user id for crashlytics
        Crashlytics.setUserIdentifier(mMerchant.getAuto_id());
    }

    /*
    private void loadMerchant() throws BackendlessException {
        LogMy.d(TAG, "In loadMerchant");
        ArrayList<String> relationProps = new ArrayList<>();
        relationProps.add("merchant");
        relationProps.add("merchant.trusted_devices");
        relationProps.add("merchant.address");
        relationProps.add("merchant.address.city");
        relationProps.add("merchant.buss_category");
        Backendless.Data.of( BackendlessUser.class ).loadRelations(mBackendlessUser, relationProps);
        mMerchant = (Merchants)mBackendlessUser.getProperty("merchant");
        LogMy.d(TAG,"Merchant loaded successfully");

        // map cashback and transaction table
        Backendless.Data.mapTableToClass(mMerchant.getCashback_table(), Cashback.class);
        Backendless.Data.mapTableToClass(mMerchant.getTxn_table(), Transaction.class);

        // Set user id for crashlytics
        Crashlytics.setUserIdentifier(mMerchant.getAuto_id());
        */
        /*
        try {
            Backendless.Data.of( BackendlessUser.class ).loadRelations(mBackendlessUser, relationProps);
            mMerchant = (Merchants)mBackendlessUser.getProperty("merchant");
        } catch (BackendlessException e) {
            LogMy.e(TAG,"loadMerchant failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;*//*
    }*/


}
