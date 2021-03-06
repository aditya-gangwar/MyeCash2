package in.myecash.merchantbase.entities;

/**
 * Created by adgangwa on 19-02-2016.
 */

import android.graphics.Bitmap;
import android.os.SystemClock;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.exceptions.BackendlessException;
import com.backendless.files.BackendlessFile;
import com.crashlytics.android.Crashlytics;

import in.myecash.appbase.backendAPI.CommonServices;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.common.CsvConverter;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.MerchantDevice;
import in.myecash.common.database.MerchantOps;
import in.myecash.common.database.MerchantOrders;
import in.myecash.common.database.MerchantStats;
import in.myecash.common.database.Merchants;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.backendAPI.MerchantServices;
import in.myecash.merchantbase.backendAPI.MerchantServicesNoLogin;

import java.io.File;
import java.util.List;

public class MerchantUser
{
    private static final String TAG = "MchntApp-MerchantUser";

    private static MerchantUser mInstance;

    // instance members
    private Bitmap mDisplayImage;
    private String mUserToken;

    private Merchants mMerchant;
    private boolean mPseudoLoggedIn;

    // fields that can be changed during lifecycle
    private String mNewCbRate;
    private int mNewIsAddClEnabled;
    private String mNewEmail;
    private String mNewContactPhone;
    private Boolean mNewInvNumAsk;
    private Boolean mNewInvNumOptional;
    private Boolean mNewInvNumOnlyNumbers;
    private String mNewPpCbRate;
    private Integer mNewPpMinAmt;

    /*
     * Singleton class
     */
    private MerchantUser(){
    }

    public static MerchantUser getInstance() {
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
        LogMy.d(TAG, "In reset");
        if(mInstance!=null && !mInstance.mPseudoLoggedIn) {
            mInstance.mMerchant = null;
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
        boolean loginSuccess = false;
        try {
            String deviceInfo = deviceId+","
                    + AppCommonUtil.getDeviceManufacturer()+","
                    + AppCommonUtil.getDeviceModel()+","
                    + AppCommonUtil.getAndroidVersion();

            LogMy.d(TAG, "Not Calling setDeviceForLogin: "+userId+", "+deviceInfo);
            //MerchantServicesNoLogin.getInstance().setDeviceForLogin(userId, deviceInfo, otp);
            //LogMy.d(TAG,"setDeviceForLogin success");

            //mInstance.mBackendlessUser = Backendless.UserService.login(userId, password, false);
            BackendlessUser user = Backendless.UserService.login(userId, password, false);
            if(  DbConstants.USER_TYPE_MERCHANT != (Integer)user.getProperty("user_type") ) {
                // wrong user type
                logoutSync();
                return ErrorCodes.USER_WRONG_ID_PASSWD;
            }

            // create instance of MerchantUser class
            loginSuccess = true;
            createInstance();
            mInstance.mMerchant = (Merchants) user.getProperty("merchant");
            mInstance.initWithMchntObject();

            // load all child objects
            // some time, explicit fetch in loadMerchant() was not working
            //mInstance.loadMerchant(userId);

            // Store user token
            mInstance.mUserToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
            if(mInstance.mUserToken == null || mInstance.mUserToken.isEmpty()) {
                logoutSync();
                return ErrorCodes.GENERAL_ERROR;
            }

            int retStatus = AppCommonUtil.loadGlobalSettings(MyGlobalSettings.RunMode.appMerchant);
            if( retStatus!= ErrorCodes.NO_ERROR) {
                logoutSync();
                return retStatus;
            }

            LogMy.d(TAG, "Login Success: " + mInstance.mMerchant.getAuto_id()+", "+mInstance.mUserToken);

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Login failed: "+e.toString());
            if(loginSuccess) {
                logoutSync();
            }
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

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception while pseudo login: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public static int logoutSync() {
        LogMy.d(TAG, "In logoutSync");

        if(mInstance!=null && !mInstance.mPseudoLoggedIn) {
            try {
                Backendless.UserService.logout();
                LogMy.d(TAG, "Logout Success");
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
            isLoginValid();
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
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        try {
            isLoginValid();
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
            if(mNewContactPhone==null) {
                mNewContactPhone = mMerchant.getContactPhone();
            }
            if(mNewInvNumAsk==null) {
                mNewInvNumAsk = mMerchant.isInvoiceNumAsk();
            }
            if(mNewInvNumOptional==null) {
                mNewInvNumOptional = mMerchant.isInvoiceNumOptional();
            }
            if(mNewInvNumOnlyNumbers==null) {
                mNewInvNumOnlyNumbers = mMerchant.isInvoiceNumOnlyNumbers();
            }
            if(mNewPpCbRate==null) {
                mNewPpCbRate = mMerchant.getPrepaidCbRate();
            }
            if(mNewPpMinAmt==null) {
                mNewPpMinAmt = mMerchant.getPrepaidCbMinAmt();
            }

            mMerchant = MerchantServices.getInstance().updateSettings(mNewCbRate, newAddClEnabled, mNewEmail, mNewContactPhone,
                    mNewInvNumAsk, mNewInvNumOptional, mNewInvNumOnlyNumbers,
                    mNewPpCbRate, mNewPpMinAmt);
            LogMy.d(TAG,"updateSettings success");

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Merchant settings update failed: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }

        // reset new setting variables - for both error and success scenarios
        mNewCbRate = null;
        mNewIsAddClEnabled = CommonConstants.BOOLEAN_VALUE_INVALID;
        mNewEmail = null;
        mNewInvNumAsk = null;
        mNewInvNumOptional = null;
        mNewInvNumOnlyNumbers = null;
        mNewPpCbRate = null;
        mNewPpMinAmt = null;

        return ErrorCodes.NO_ERROR;
    }

    public int changeMobileNum(String verifyparam, String newMobile, String otp) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        int returnCode = ErrorCodes.NO_ERROR;
        try {
            isLoginValid();
            LogMy.d(TAG,"Before changeMobileNum: "+mMerchant.getMobile_num());
            mMerchant = MerchantServices.getInstance().changeMobile(verifyparam, newMobile, otp);
            LogMy.d(TAG,"changeMobileNum success: "+mMerchant.getMobile_num());

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
    public String executeCustOp(MyCustomerOps custOp) {
        if(mPseudoLoggedIn) {
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
        }
        isLoginValid();

        return CommonServices.getInstance().execCustomerOp(custOp.getOp_code(),custOp.getMobile_num(),custOp.getQr_card(),
                custOp.getOtp(),custOp.getPin(),custOp.getExtra_op_params());
    }

    public Cashback registerCustomer(String mobileNum, String dob, int sex, String qrCode, String otp, String firstName, String lastName) {
        if(mPseudoLoggedIn) {
            // intentionally using 'Backend' error code - as calling fx. will try to convert
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
        }
        isLoginValid();
        return MerchantServices.getInstance().registerCustomer(mobileNum, dob, sex, qrCode, otp, firstName, lastName);
    }

    /*
     * Methods for DB fetches
     */
    public MerchantStats fetchStats() throws BackendlessException {
        isLoginValid();
        return MerchantServices.getInstance().getMerchantStats(mMerchant.getAuto_id());
    }

    public Cashback fetchCashback(String custId) throws BackendlessException {
        LogMy.d(TAG, "In fetchCashback");
        isLoginValid();

        return MerchantServices.getInstance().getCashback(
                mMerchant.getAuto_id(),
                mMerchant.getCashback_table(),
                custId,
                mMerchant.getDebugLogs());
    }

    public List<MerchantOps> fetchMerchantOps() throws BackendlessException {
        LogMy.d(TAG, "In fetchMerchantOps");
        isLoginValid();

        return MerchantServices.getInstance().getMerchantOps(
                mMerchant.getAuto_id());
    }

    /*
     * Methods for DB uploads / commits
     */
    public int startLoad(String customerId, String pin, int reps) {
        if(mPseudoLoggedIn || !AppConstants.DEBUG_MODE) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }

        int errCode = ErrorCodes.NO_ERROR;
        int txnDone = 0;

        try {
            Transaction trans = new Transaction();
            trans.setCust_private_id(customerId);
            trans.setUsedCardId("");
            trans.setCb_percent("25");
            trans.setExtra_cb_credit(0);
            trans.setExtra_cb_percent("0");

            MyTransaction myTxn = new MyTransaction(trans);

            // 1000 txn commits for given customer
            for(int i=1; i<=reps; i++) {

                SystemClock.sleep(1000);
                switch (i%4) {
                    case 1:
                        LogMy.d(TAG,"Case 1");
                        trans.setTotal_billed(1000);
                        trans.setCb_billed(1000);
                        trans.setCb_credit(250);
                        trans.setCb_debit(0);
                        trans.setCl_credit(0);
                        trans.setCl_debit(0);
                        break;

                    case 2:
                        LogMy.d(TAG,"Case 2");
                        trans.setTotal_billed(250);
                        trans.setCb_billed(0);
                        trans.setCb_credit(0);
                        trans.setCb_debit(250);
                        trans.setCl_credit(0);
                        trans.setCl_debit(0);
                        break;

                    case 3:
                        LogMy.d(TAG,"Case 3");
                        trans.setTotal_billed(0);
                        trans.setCb_billed(0);
                        trans.setCb_credit(0);
                        trans.setCb_debit(0);
                        trans.setCl_credit(100);
                        trans.setCl_debit(0);
                        break;

                    case 0:
                        LogMy.d(TAG,"Case 0");
                        trans.setTotal_billed(0);
                        trans.setCb_billed(0);
                        trans.setCb_credit(0);
                        trans.setCb_debit(0);
                        trans.setCl_credit(0);
                        trans.setCl_debit(100);
                        break;

                    default:
                        LogMy.e(TAG,"Invalid value: "+i%4);
                }
                errCode = commitTxn(myTxn, pin, false, true);
                if(errCode!=ErrorCodes.NO_ERROR) {
                    break;
                } else {
                    txnDone++;
                }
            } // end loop
        } catch( BackendlessException e ) {
            LogMy.e(TAG, "Commit cash transaction in load failed: " + e.toString());
            errCode = AppCommonUtil.getLocalErrorCode(e);
        }

        LogMy.d(TAG,"Committed "+txnDone+" txns");
        return errCode;
    }

    public int commitTxn(MyTransaction txn, String pin, boolean isOtp, boolean loadTest) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }
        try {
            isLoginValid();
            //txn.getTransaction().setCpin(pin);

            Transaction tx = txn.getTransaction();
            // settings values eventually set by backend to null/empty for now
            tx.setTrans_id("");
            tx.setMerchant_id("");
            tx.setMerchant_name("");
            tx.setCust_mobile("");

            String csvStr = CsvConverter.csvStrFromTxn(tx);
            Transaction newTxn = MerchantServices.getInstance().commitTxn(csvStr,pin,isOtp);
            LogMy.d(TAG, "Txn commit success: " + newTxn.getTrans_id());
            if(!loadTest) {
                txn.setCurrTransaction(newTxn);
            }
            //txn.commit();

        } catch( BackendlessException e ) {
            LogMy.e(TAG, "Commit cash transaction failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public int cancelTxn(MyTransaction txn, String cardId, String pin, boolean isOtp) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }
        try {
            isLoginValid();
            Transaction newTxn = MerchantServices.getInstance().cancelTxn(txn.getTransaction().getTrans_id(), cardId, pin, isOtp);
            LogMy.d(TAG, "Txn cancel success: " + newTxn.getTrans_id());
            txn.setCurrTransaction(newTxn);

        } catch(BackendlessException e) {
            LogMy.e(TAG, "Txn cancel failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public int genTxnOtp(String custMobileOrId) {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }
        try {
            MerchantServices.getInstance().generateTxnOtp(custMobileOrId);
            LogMy.d(TAG, "Generate OTP success: " + custMobileOrId);

        } catch(BackendlessException e) {
            LogMy.e(TAG, "Generate OTP failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    /*
     * Merchant Order Fxs
     */
    public List<MerchantOrders> fetchMchntOrders() throws BackendlessException {
        LogMy.d(TAG, "In fetchMchntOrders");
        return CommonServices.getInstance().getMchntOrders(mMerchant.getAuto_id(),"","");
    }

    public MerchantOrders createMchntOrder(String sku, int qty, int totalPrice) {
        if(mPseudoLoggedIn) {
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
        }
        MerchantOrders order = MerchantServices.getInstance().createMchntOrder(sku, qty, totalPrice);
        LogMy.d(TAG, "Mchnt Order Create Success: " + order.getOrderId());
        return order;
    }

    public int deleteMchntOrder(String orderId) {
        LogMy.d(TAG, "In deleteMchntOrder: " + orderId);
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }
        try {
            MerchantServices.getInstance().deleteMchntOrder(orderId);
            LogMy.d(TAG, "Mchnt Order delete success: " + orderId);

        } catch(BackendlessException e) {
            LogMy.e(TAG, "Mchnt Order delete failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }



    public void uploadImgFile(File file, String remoteDir) throws Exception {
        if(mPseudoLoggedIn) {
            throw new BackendlessException(String.valueOf(ErrorCodes.OPERATION_NOT_ALLOWED), "");
        }
        isLoginValid();
        // upload file
        //String dir = remoteDir+CommonConstants.FILE_PATH_SEPERATOR;
        BackendlessFile newfile = Backendless.Files.upload(file, remoteDir, true);
        LogMy.d(TAG, "Image uploaded successfully at :" + newfile.getFileURL());
    }

    /*
     * Methods for other DB actions
     */
    public int archiveTxns() {
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
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

    public int deleteTrustedDevice(int index, String curDeviceId) {
        LogMy.d(TAG, "In deleteTrustedDevice: " + index);
        if(mPseudoLoggedIn) {
            return ErrorCodes.OPERATION_NOT_ALLOWED;
        }
        try {
            isLoginValid();
            MerchantServices.getInstance().deleteTrustedDevice(mMerchant.getTrusted_devices().get(index).getDevice_id(),curDeviceId);
            LogMy.d(TAG, "Device delete success: " + mMerchant.getAuto_id());
            mInstance.loadMerchant(mMerchant.getAuto_id());

        } catch(BackendlessException e) {
            LogMy.e(TAG, "Device delete failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    /*
     * Public methods for changing merchant properties/settings
     */
    public void setNewEmail(String newEmail) {
        mNewEmail = newEmail;
    }

    public void setNewContactPhome(String newPhone) {
        mNewContactPhone = newPhone;
    }

    public void setNewIsAddClEnabled(boolean newIsAddClEnabled) {
        mNewIsAddClEnabled = newIsAddClEnabled ? CommonConstants.BOOLEAN_VALUE_TRUE : CommonConstants.BOOLEAN_VALUE_FALSE;
    }

    public void setNewCbRate(String newCbRate) {
        mNewCbRate = newCbRate;
    }

    public void setNewInvNumAsk(boolean newInvNumAsk) {
        this.mNewInvNumAsk = newInvNumAsk;
    }

    public void setNewInvNumOptional(boolean newInvNumOptional) {
        this.mNewInvNumOptional = newInvNumOptional;
    }

    public void setNewInvNumOnlyNumbers(boolean newInvNumOnlyNumbers) {
        this.mNewInvNumOnlyNumbers = newInvNumOnlyNumbers;
    }

    public void setNewPpMinAmt(Integer mNewPpMinAmt) {
        this.mNewPpMinAmt = mNewPpMinAmt;
    }

    public void setNewPpCbRate(String mNewPpCbRate) {
        this.mNewPpCbRate = mNewPpCbRate;
    }

    /*
         * Getter fxs
         */
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

    public List<MerchantDevice> getTrustedDeviceList() {
        return mMerchant.getTrusted_devices();
    }

    public Merchants getMerchant()
    {
        return mMerchant;
    }

    public boolean isPseudoLoggedIn() {
        return mPseudoLoggedIn;
    }

    /*
     * Private helper functions
     */
    private void isLoginValid() {
        // Not working properly - so commenting it out
        //String userToken = UserTokenStorageFactory.instance().getStorage().get();
        /*String userToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
        if(userToken==null || userToken.isEmpty()) {
            LogMy.e(TAG,"User token is null. Auto logout scenario");
            throw new BackendlessException(String.valueOf(ErrorCodes.NOT_LOGGED_IN), "");
        }*/
    }

    private void loadMerchant(String idOrMobileNum) {
        mMerchant = CommonServices.getInstance().getMerchant(idOrMobileNum);
        initWithMchntObject();
    }

    private void initWithMchntObject() {
        LogMy.d(TAG,mMerchant.getCashback_table()+", "+mMerchant.getTxn_table());

        // map cashback and transaction table
        Backendless.Data.mapTableToClass(mMerchant.getCashback_table(), Cashback.class);
        Backendless.Data.mapTableToClass(mMerchant.getTxn_table(), Transaction.class);

        // Set user id for crashlytics
        if(AppConstants.USE_CRASHLYTICS) {
            Crashlytics.setUserIdentifier(mMerchant.getAuto_id());
        }
    }

}
