package in.myecash.merchantbase.helper;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;

import in.myecash.appbase.backendAPI.CommonServices;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.Customers;
import in.myecash.common.database.MerchantOrders;
import in.myecash.common.database.MerchantStats;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.FileFetchr;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.backendAPI.MerchantServices;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.appbase.entities.MyTransaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by adgangwa on 27-02-2016.
 */
public class MyBackgroundProcessor<T> extends BackgroundProcessor<T> {
    private final static String TAG = "MchntApp-MyBackgroundProcessor";

    private MyRetainedFragment mRetainedFragment;

    public MyBackgroundProcessor(Handler responseHandler, MyRetainedFragment retainedFragment) {
        super(responseHandler);
        mRetainedFragment = retainedFragment;
    }

    private class MessageLogin implements Serializable {
        public String userId;
        public String passwd;
        public String deviceId;
        public String otp;
    }
    private class MessageResetPassword implements Serializable {
        public String userId;
        public String brandName;
        public String deviceId;
    }
    private class MessageCustRegister implements Serializable {
        public String firstName;
        public String lastName;
        public String mobileNum;
        public String otp;
        public String qrCode;
        public String dob;
        public int sex;
    }
    private class MessageDelDevice implements Serializable {
        public String curDeviceId;
        public int index;
    }
    private class MessageChangePassword implements Serializable {
        public String oldPasswd;
        public String newPasswd;
    }
    private class MessageFileDownload implements Serializable {
        public Context ctxt;
        public String fileUrl;
    }
    private class MessageForgotId implements Serializable {
        public String mobileNum;
        public String deviceId;
    }
    private class MessageCancelTxn implements Serializable {
        public String txnId;
        public String cardId;
        public String pin;
        public boolean isOtp;
    }
    private class MessageTxnCommit implements Serializable {
        public String pin;
        public boolean isOtp;
    }
    private class MessageImgUpload implements Serializable {
        public File file;
        public String remoteDir;
    }
    private class MessageMcntOrder implements Serializable {
        public String skuOrId;
        public int qty;
        public int totalPrice;
    }
    private class MessageLoadTest implements Serializable {
        public String custId;
        public String pin;
        public int reps;
    }

    /*
     * Add request methods - Assumes that MerchantUser is instantiated
     */
    public void addMerchantOpsReq() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_MERCHANT_OPS, null).sendToTarget();
    }

    public void addArchiveTxnsRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_ARCHIVE_TXNS,null).sendToTarget();
    }

    public void addMerchantStatsRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_MERCHANT_STATS,null).sendToTarget();
    }

    public void addDeleteDeviceRequest(Integer index, String curDeviceId) {
        MessageDelDevice msg = new MessageDelDevice();
        msg.curDeviceId = curDeviceId;
        msg.index = index;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DELETE_TRUSTED_DEVICE, msg).sendToTarget();
    }

    public void addChangeMobileRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_MOBILE,null).sendToTarget();
    }
    public void addImgUploadRequest(File file, String remoteDir) {
        LogMy.d(TAG, "In addImgUploadRequest");
        MessageImgUpload msg = new MessageImgUpload();
        msg.file = file;
        msg.remoteDir = remoteDir;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_UPLOAD_IMG, msg).sendToTarget();
    }
    public void changePassword(String oldPasswd, String newPasswd) {
        LogMy.d(TAG, "In changePassword:  ");
        MessageChangePassword msg = new MessageChangePassword();
        msg.oldPasswd = oldPasswd;
        msg.newPasswd = newPasswd;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_PASSWD,msg).sendToTarget();
    }
    public void addCustomerOp() {
        LogMy.d(TAG, "In addCustomerOp");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_ADD_CUSTOMER_OP,null).sendToTarget();
    }
    public void addPasswordRequest(String brandName, String deviceId, String userId) {
        LogMy.d(TAG, "In addPasswordRequest");
        MessageResetPassword msg = new MessageResetPassword();
        msg.brandName = brandName;
        msg.deviceId = deviceId;
        msg.userId = userId;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_GENERATE_MERCHANT_PWD, msg).sendToTarget();
    }
    public void addForgotIdRequest(String mobileNum, String deviceId) {
        LogMy.d(TAG, "In addForgotIdRequest");
        MessageForgotId msg = new MessageForgotId();
        msg.deviceId = deviceId;
        msg.mobileNum = mobileNum;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FORGOT_ID, msg).sendToTarget();
    }
    public void addCommitTransRequest(String pin, boolean isOtp) {
        LogMy.d(TAG, "In addCommitTransRequest");
        MessageTxnCommit msg = new MessageTxnCommit();
        msg.pin = pin;
        msg.isOtp = isOtp;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_COMMIT_TRANS, msg).sendToTarget();
    }
    public void addCashbackRequest(String custId) {
        LogMy.d(TAG, "In addCashbackRequest:  " + custId);
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_GET_CASHBACK, custId).sendToTarget();
    }
    public void addCustRegRequest(String mobileNum, String dob, int sex, String qrCode, String otp, String firstName, String lastName) {
        LogMy.d(TAG, "In addCustRegRequest:  " + mobileNum);
        MessageCustRegister msg = new MessageCustRegister();
        msg.firstName = firstName;
        msg.lastName = lastName;
        msg.mobileNum = mobileNum;
        msg.otp = otp;
        msg.qrCode = qrCode;
        msg.dob = dob;
        msg.sex = sex;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_REGISTER_CUSTOMER,msg).sendToTarget();
    }
    public void addLoginRequest(String userId, String password, String deviceId, String otp) {
        LogMy.d(TAG, "In addLoginRequest");
        MessageLogin msg = new MessageLogin();
        msg.userId = userId;
        msg.deviceId = deviceId;
        msg.passwd = password;
        msg.otp = otp;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_LOGIN, msg).sendToTarget();
    }
    public void addMerchantSettingsRequest() {
        LogMy.d(TAG, "In addMerchantSettingsRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_UPDATE_MERCHANT_SETTINGS).sendToTarget();
    }
    public void addLogoutRequest() {
        LogMy.d(TAG, "In addLogoutRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_LOGOUT_MERCHANT).sendToTarget();
    }
    public void addFetchTxnsRequest(String query) {
        LogMy.d(TAG, "In addFetchTxnsRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_TXNS, query).sendToTarget();
    }
    public void addFetchTxnFilesRequest(Context context) {
        LogMy.d(TAG, "In addFetchTxnFilesRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_TXN_FILES, context).sendToTarget();
    }
    public void addCustFileDownloadReq(Context context, String fileURL) {
        LogMy.d(TAG, "In addFileDownloadRequest: " + fileURL);
        MessageFileDownload msg = new MessageFileDownload();
        msg.ctxt = context;
        msg.fileUrl = fileURL;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CUST_DATA_FILE_DOWNLOAD, msg).sendToTarget();
    }
    public void addCancelTxnReq(String txnId, String cardId, String pin, boolean isOtp) {
        MessageCancelTxn msg = new MessageCancelTxn();
        msg.cardId = cardId;
        msg.txnId = txnId;
        msg.pin = pin;
        msg.isOtp = isOtp;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CANCEL_TXN, msg).sendToTarget();
    }
    public void addGenTxnOtpReq(String custMobileOrId) {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_GEN_TXN_OTP, custMobileOrId).sendToTarget();
    }
    public void createMchntOrder(String sku, int qty, int totalPrice) {
        MessageMcntOrder msg = new MessageMcntOrder();
        msg.qty = qty;
        msg.skuOrId = sku;
        msg.totalPrice = totalPrice;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CRT_MCHNT_ORDER, msg).sendToTarget();
    }
    public void addFetchMchntOrderReq() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_MERCHANT_ORDERS, null).sendToTarget();
    }
    public void addDeleteMchntOrder(String orderId) {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DELETE_MCHNT_ORDER, orderId).sendToTarget();
    }

    public void addLoadTestReq(String custId, String pin, int reps) {
        MessageLoadTest msg = new MessageLoadTest();
        msg.custId = custId;
        msg.pin = pin;
        msg.reps = reps;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_LOAD_TEST, msg).sendToTarget();
    }

    @Override
    protected int handleMsg(Message msg) {
        int error = ErrorCodes.NO_ERROR;
        try {

            // It checks with internet site - so checking only during login
            /*if( msg.what==MyRetainedFragment.REQUEST_LOGIN &&
                    !AppCommonUtil.isInternetConnected()) {
                return ErrorCodes.NO_INTERNET_CONNECTION;
            }*/

            switch (msg.what) {
                case MyRetainedFragment.REQUEST_GET_CASHBACK:
                    error = getCashback((String) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_LOGIN:
                    error = loginMerchant((MessageLogin) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_REGISTER_CUSTOMER:
                    error = registerCustomer((MessageCustRegister) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_COMMIT_TRANS:
                    //commitCashTrans((Transaction) msg.obj);
                    error = commitCashTrans((MessageTxnCommit) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_UPDATE_MERCHANT_SETTINGS:
                    error = updateMerchantSettings();
                    break;
                case MyRetainedFragment.REQUEST_LOGOUT_MERCHANT:
                    error = logoutMerchant();
                    break;
                case MyRetainedFragment.REQUEST_FETCH_TXNS:
                    error = fetchTransactions((String) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_FETCH_TXN_FILES:
                    error = fetchTxnFiles((Context) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_GENERATE_MERCHANT_PWD:
                    error = generatePassword((MessageResetPassword) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_ADD_CUSTOMER_OP:
                    error = executeCustOp();
                    break;
                case MyRetainedFragment.REQUEST_CHANGE_PASSWD:
                    error = changePassword((MessageChangePassword) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_UPLOAD_IMG:
                    error = uploadImgFile((MessageImgUpload) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_DELETE_TRUSTED_DEVICE:
                    error = deleteDevice((MessageDelDevice) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_CHANGE_MOBILE:
                    error = changeMobileNum();
                    break;
                case MyRetainedFragment.REQUEST_MERCHANT_STATS:
                    error = fetchMerchantStats();
                    break;
                case MyRetainedFragment.REQUEST_FORGOT_ID:
                    error = forgotId((MessageForgotId) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_ARCHIVE_TXNS:
                    error = archiveTxns();
                    break;
                case MyRetainedFragment.REQUEST_CUST_DATA_FILE_DOWNLOAD:
                    error = downloadFile((MessageFileDownload) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_FETCH_MERCHANT_OPS:
                    error = fetchMerchantOps();
                    break;
                case MyRetainedFragment.REQUEST_CANCEL_TXN:
                    error = cancelTxn((MessageCancelTxn) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_CRT_MCHNT_ORDER:
                    error = createMchntOrder((MessageMcntOrder) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_FETCH_MERCHANT_ORDERS:
                    error = fetchMchntOrders();
                    break;
                case MyRetainedFragment.REQUEST_DELETE_MCHNT_ORDER:
                    error = deleteMchntOrder((String) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_GEN_TXN_OTP:
                    error = genTxnOtp((String) msg.obj);
                    break;
                case MyRetainedFragment.REQUEST_LOAD_TEST:
                    MessageLoadTest data = (MessageLoadTest)msg.obj;
                    error = MerchantUser.getInstance().startLoad(data.custId, data.pin, data.reps);
                    break;
            }
        } catch (Exception e) {
            LogMy.e(TAG,"Unhandled exception in BG thread", e);
            error = ErrorCodes.GENERAL_ERROR;
        }
        return error;
    }

    private int fetchMerchantOps() {
        mRetainedFragment.mLastFetchMchntOps = null;

        try {
            mRetainedFragment.mLastFetchMchntOps = MerchantUser.getInstance().fetchMerchantOps();
            LogMy.d(TAG,"fetchMerchantOps success: "+mRetainedFragment.mLastFetchMchntOps.size());

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in fetchMerchantOps: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int archiveTxns() {
        return MerchantUser.getInstance().archiveTxns();
    }

    private int loginMerchant(MessageLogin msg) {
        LogMy.d(TAG, "In loginMerchant");
        return MerchantUser.login(msg.userId, msg.passwd, msg.deviceId, msg.otp);
    }
    
    private int fetchMerchantStats() {
        mRetainedFragment.mMerchantStats = null;
        try {
            MerchantStats stats = MerchantUser.getInstance().fetchStats();
            mRetainedFragment.mMerchantStats = stats;

            long updateTime = (mRetainedFragment.mMerchantStats.getUpdated()==null) ?
                    mRetainedFragment.mMerchantStats.getCreated().getTime() :
                    mRetainedFragment.mMerchantStats.getUpdated().getTime();
            LogMy.d(TAG,"getMerchantStats success: "+updateTime);

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in fetchMerchantStats: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int changeMobileNum() {
        return MerchantUser.getInstance().changeMobileNum(mRetainedFragment.mVerifyParamMobileChange,
                mRetainedFragment.mNewMobileNum, mRetainedFragment.mOtpMobileChange);
    }

    private int deleteDevice(MessageDelDevice msg) {
        return MerchantUser.getInstance().deleteTrustedDevice(msg.index, msg.curDeviceId);
    }

    private int uploadImgFile(MessageImgUpload msg) {
        File file = msg.file;
        try {
            MerchantUser.getInstance().uploadImgFile(file, msg.remoteDir);

        } catch (BackendlessException e) {
            LogMy.e(TAG,"BackendlessException in uploadImgFile: "+file.getAbsolutePath()+", "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);

        } catch(Exception e) {
            LogMy.e(TAG,"Exception in uploadImgFile: "+file.getAbsolutePath()+", "+e.toString(),e);
            return ErrorCodes.GENERAL_ERROR;
        } finally {
            // delete local file
            if(!file.delete()) {
                LogMy.w(TAG,"Failed to delete local image file: "+file.getAbsolutePath());
            }
        }
        return ErrorCodes.NO_ERROR;
    }

    private int changePassword(MessageChangePassword data) {
        return MerchantUser.getInstance().changePassword(data.oldPasswd, data.newPasswd);
    }

    private int executeCustOp() {
        try
        {
            String imgFilename = MerchantUser.getInstance().executeCustOp(mRetainedFragment.mCustomerOp);
            LogMy.d(TAG,"executeCustOp returned img filename as: "+imgFilename);
            mRetainedFragment.mCustomerOp.setImageFilename(imgFilename);
        }
        catch( BackendlessException e )
        {
            int errCode = AppCommonUtil.getLocalErrorCode(e);
            if(errCode==ErrorCodes.OP_SCHEDULED) {
                // Retrieve imgFilename from exception message
                //String imgFilename = e.getMessage();
                //LogMy.d(TAG,"executeCustOp returned img filename as: "+imgFilename);
                mRetainedFragment.mCustomerOp.setImageFilename(AppCommonUtil.mErrorParams.imgFileName);

            } else if(errCode!=ErrorCodes.OTP_GENERATED) {
                LogMy.e(TAG, "exec customer op failed: "+ e.toString());
            }
            return errCode;
        }
        return ErrorCodes.NO_ERROR;
    }

    private int generatePassword(MessageResetPassword msg) {
        return MerchantUser.resetPassword(msg.brandName, msg.userId, msg.deviceId);
    }

    private int forgotId(MessageForgotId msg) {
        return MerchantUser.forgotId(msg.mobileNum, msg.deviceId);
    }

    private int registerCustomer(MessageCustRegister data) {
        mRetainedFragment.mCurrCashback = null;
        mRetainedFragment.mCurrCustomer = null;

        try {
            Cashback cashback = MerchantUser.getInstance().registerCustomer(data.mobileNum, data.dob, data.sex, data.qrCode,
                    data.otp, data.firstName, data.lastName);

            mRetainedFragment.mCurrCashback = new MyCashback();
            mRetainedFragment.mCurrCashback.init(cashback, true);
            mRetainedFragment.mCurrCustomer = mRetainedFragment.mCurrCashback.getCustomer();

        } catch (BackendlessException e) {
            mRetainedFragment.mCurrCashback = null;
            mRetainedFragment.mCurrCustomer = null;

            int error = AppCommonUtil.getLocalErrorCode(e);
            if(error!=ErrorCodes.OTP_GENERATED) {
                LogMy.e(TAG, "Exception in registerCustomer: "+ e.toString());
            }
            return error;
        }
        return ErrorCodes.NO_ERROR;
    }

    private int getCashback(String custId) {
        mRetainedFragment.mCurrCashback = null;
        mRetainedFragment.mCurrCustomer = null;

        try {
            Cashback cashback = MerchantUser.getInstance().fetchCashback(custId);

            mRetainedFragment.mCurrCashback = new MyCashback();
            mRetainedFragment.mCurrCashback.init(cashback, true);
            mRetainedFragment.mCurrCustomer = mRetainedFragment.mCurrCashback.getCustomer();

        } catch (BackendlessException e) {
            mRetainedFragment.mCurrCashback = null;
            mRetainedFragment.mCurrCustomer = null;

            LogMy.e(TAG, "Exception in getCashback: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int commitCashTrans(MessageTxnCommit msg) {
        int errorCode =  MerchantUser.getInstance().commitTxn(mRetainedFragment.mCurrTransaction, msg.pin, msg.isOtp, false);
        if(errorCode==ErrorCodes.NO_ERROR) {
            mRetainedFragment.mCurrCashback.setCashback(mRetainedFragment.mCurrTransaction.getTransaction().getCashback());
        }
        return errorCode;
    }

    private int genTxnOtp(String custMobileOrId) {
        return MerchantUser.getInstance().genTxnOtp(custMobileOrId);
    }

    private int updateMerchantSettings() {
        return MerchantUser.getInstance().updateSettings();
    }

    private int logoutMerchant() {
        return MerchantUser.logoutSync();
    }

    private int fetchTransactions(String query) {
        mRetainedFragment.mLastFetchTransactions = null;

        try {
            isSessionValid();
            mRetainedFragment.mLastFetchTransactions = MyTransaction.fetch(query, MerchantUser.getInstance().getMerchant().getTxn_table());
            if (mRetainedFragment.mLastFetchTransactions == null || mRetainedFragment.mLastFetchTransactions.size() == 0) {
                return ErrorCodes.NO_DATA_FOUND;
            }
            return ErrorCodes.NO_ERROR;

        } catch (BackendlessException e) {
            mRetainedFragment.mLastFetchTransactions = null;
            LogMy.e(TAG, "Exception in fetchTransactions: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
    }

    private int cancelTxn(MessageCancelTxn msg) {
        //return MerchantUser.getInstance().cancelTxn(msg.txnId, msg.cardId, msg.pin);
        return MerchantUser.getInstance().cancelTxn(mRetainedFragment.mCurrTransaction, msg.cardId, msg.pin, msg.isOtp);
    }

    private int fetchMchntOrders() {
        mRetainedFragment.mLastFetchMchntOrders = null;

        try {
            mRetainedFragment.mLastFetchMchntOrders = MerchantUser.getInstance().fetchMchntOrders();
            LogMy.d(TAG,"fetchMchntOrders success: "+mRetainedFragment.mLastFetchMchntOrders.size());

            // sort by time
            Collections.sort(mRetainedFragment.mLastFetchMchntOrders, new AppCommonUtil.MchntOrderComparator());
            Collections.reverse(mRetainedFragment.mLastFetchMchntOrders);

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Exception in fetchMchntOrders: "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int createMchntOrder(MessageMcntOrder msg) {
        try {
            MerchantOrders order = MerchantUser.getInstance().createMchntOrder(msg.skuOrId, msg.qty, msg.totalPrice);

            if(mRetainedFragment.mLastFetchMchntOrders==null) {
                mRetainedFragment.mLastFetchMchntOrders = new ArrayList<>();
            }
            mRetainedFragment.mLastFetchMchntOrders.add(order);

            // sort by time
            Collections.sort(mRetainedFragment.mLastFetchMchntOrders, new AppCommonUtil.MchntOrderComparator());
            Collections.reverse(mRetainedFragment.mLastFetchMchntOrders);

        } catch (BackendlessException e) {
            LogMy.e(TAG, "Exception in createMchntOrder: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int deleteMchntOrder(String orderId) {
        int status = MerchantUser.getInstance().deleteMchntOrder(orderId);
        if(status==ErrorCodes.NO_ERROR) {
            // remove from local list also
            Iterator<MerchantOrders> it = mRetainedFragment.mLastFetchMchntOrders.iterator();
            while (it.hasNext()) {
                if (it.next().getOrderId().equals(orderId)) {
                    it.remove();
                    break;
                }
            }
        }
        return status;
    }



    private int fetchTxnFiles(Context ctxt) {
        int errorCode = ErrorCodes.NO_ERROR;

        try {
            isSessionValid();
        } catch (BackendlessException e) {
            return AppCommonUtil.getLocalErrorCode(e);
        }

        // create a copy of list
        List<String> missingFiles = new ArrayList<>(mRetainedFragment.mMissingFiles);

        MessageFileDownload msg = new MessageFileDownload();
        for(int i=0; i<missingFiles.size(); i++) {
            // convert filepath to complete URL
            //https://api.backendless.com/09667f8b-98a7-e6b9-ffeb-b2b6ee831a00/v1/files/<filepath>
            msg.ctxt = ctxt;
            msg.fileUrl = missingFiles.get(i);
            errorCode = downloadFile(msg);
            //remove from missing files list
            if(errorCode==ErrorCodes.NO_ERROR) {
                LogMy.d(TAG,"Txn file found remotely, removing from missing list: "+missingFiles.get(i));
                mRetainedFragment.mMissingFiles.remove(missingFiles.get(i));
            }
        }

        return errorCode;
    }

    private int downloadFile(MessageFileDownload msg) {
        try {
            String filepath = msg.fileUrl;
            String fileURL = AppConstants.BACKEND_FILE_BASE_URL + filepath;
            //String filename = filepath.substring(filepath.lastIndexOf('/')+1);
            String filename = Uri.parse(fileURL).getLastPathSegment();
            LogMy.d(TAG,"Fetching "+fileURL+", Filename: "+filename);

            byte[] bytes = new FileFetchr().getUrlBytes(fileURL, mRetainedFragment.mUserToken);

            FileOutputStream outputStream;
            outputStream = msg.ctxt.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(bytes);
            outputStream.close();

        } catch(FileNotFoundException fnf) {
            LogMy.d(TAG, "File not found: "+fnf.toString());
            return ErrorCodes.FILE_NOT_FOUND;
        } catch(Exception e) {
            LogMy.e(TAG, "Failed to fetch file: "+e.toString());
            return ErrorCodes.GENERAL_ERROR;
        }
        return ErrorCodes.NO_ERROR;
    }

    private void isSessionValid() {
        try {
            CommonServices.getInstance().isSessionValid();
            LogMy.d(TAG,"Session is valid");
        } catch (BackendlessException e) {
            LogMy.e(TAG, "Session not valid: "+ e.toString());
            throw e;
        }
    }
}
