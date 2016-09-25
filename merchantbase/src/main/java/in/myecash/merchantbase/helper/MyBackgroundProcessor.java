package in.myecash.merchantbase.helper;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.backendless.exceptions.BackendlessException;
import in.myecash.commonbase.constants.BackendSettings;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.Cashback;
import in.myecash.commonbase.models.MerchantStats;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.BackgroundProcessor;
import in.myecash.commonbase.utilities.FileFetchr;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.commonbase.entities.MyCashback;
import in.myecash.merchantbase.entities.MyTransaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adgangwa on 27-02-2016.
 */
public class MyBackgroundProcessor<T> extends BackgroundProcessor<T> {
    private final static String TAG = "MyBackgroundProcessor";

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
        public String mobileNum;
        public String name;
        public String qrCode;
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

    public void addDeleteDeviceRequest(Integer index) {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_DELETE_TRUSTED_DEVICE, index).sendToTarget();
    }

    public void addChangeMobileRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_MOBILE,null).sendToTarget();
    }
    public void addTxnImgUploadRequest(File file) {
        LogMy.d(TAG, "In addTxnImgUploadRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_UPLOAD_TXN_IMG, file).sendToTarget();
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
    public void addCommitTransRequest(String pin) {
        LogMy.d(TAG, "In addCommitTransRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_COMMIT_TRANS, pin).sendToTarget();
    }
    public void addCashbackRequest(String custId) {
        LogMy.d(TAG, "In addCashbackRequest:  " + custId);
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_GET_CASHBACK, custId).sendToTarget();
    }
    public void addCustRegRequest(String name, String mobileNum, String qrCode) {
        LogMy.d(TAG, "In addCustRegRequest:  " + mobileNum);
        MessageCustRegister msg = new MessageCustRegister();
        msg.mobileNum = mobileNum;
        msg.name = name;
        msg.qrCode = qrCode;
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
    public void addFetchTxnFilesRequest(Context context, List<String> missingFiles) {
        LogMy.d(TAG, "In addFetchTxnFilesRequest: " + missingFiles.size());
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_TXN_FILES, context).sendToTarget();
    }
    public void addCustFileDownloadReq(Context context, String fileURL) {
        LogMy.d(TAG, "In addFileDownloadRequest: " + fileURL);
        MessageFileDownload msg = new MessageFileDownload();
        msg.ctxt = context;
        msg.fileUrl = fileURL;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CUST_DATA_FILE_DOWNLOAD, msg).sendToTarget();
    }

    @Override
    protected int handleMsg(Message msg) {
        int error = ErrorCodes.NO_ERROR;
        switch(msg.what) {
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
                error = commitCashTrans((String) msg.obj);
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
            case MyRetainedFragment.REQUEST_UPLOAD_TXN_IMG:
                error = uploadTxnImgFile((File) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_DELETE_TRUSTED_DEVICE:
                error = deleteDevice((Integer) msg.obj);
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
            LogMy.d(TAG,"getMerchantStats success");
            mRetainedFragment.mMerchantStats = stats;

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

    private int deleteDevice(Integer index) {
        return MerchantUser.getInstance().deleteTrustedDevice(index);
    }

    private int uploadTxnImgFile(File file) {
        try {
            MerchantUser.getInstance().uploadTxnImgFile(file);
            LogMy.d(TAG,"Succesfully uploaded txn image file: "+file.getName());
            // delete local file
            if(!file.delete()) {
                LogMy.w(TAG,"Failed to delete txn image file: "+file.getAbsolutePath());
            }
        } catch (BackendlessException e) {
            LogMy.e(TAG,"BackendlessException in uploadTxnImgFile: "+file.getAbsolutePath()+", "+e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        } catch(Exception e) {
            LogMy.e(TAG,"Exception in uploadTxnImgFile: "+file.getAbsolutePath()+", "+e.toString(),e);
            return ErrorCodes.GENERAL_ERROR;
        }
        return ErrorCodes.NO_ERROR;
    }

    private int changePassword(MessageChangePassword data) {
        return MerchantUser.getInstance().changePassword(data.oldPasswd, data.newPasswd);
    }

    private int executeCustOp() {
        return MerchantUser.getInstance().executeCustOp(mRetainedFragment.mCustomerOp);
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
            Cashback cashback = MerchantUser.getInstance().registerCustomer(data.mobileNum, data.name, data.qrCode);

            mRetainedFragment.mCurrCashback = new MyCashback();
            mRetainedFragment.mCurrCashback.init(cashback);
            mRetainedFragment.mCurrCustomer = mRetainedFragment.mCurrCashback.getCustomer();

        } catch (BackendlessException e) {
            mRetainedFragment.mCurrCashback = null;
            mRetainedFragment.mCurrCustomer = null;

            LogMy.e(TAG, "Exception in registerCustomer: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int getCashback(String custId) {
        mRetainedFragment.mCurrCashback = null;
        mRetainedFragment.mCurrCustomer = null;

        try {
            Cashback cashback = MerchantUser.getInstance().fetchCashback(custId);

            mRetainedFragment.mCurrCashback = new MyCashback();
            mRetainedFragment.mCurrCashback.init(cashback);
            mRetainedFragment.mCurrCustomer = mRetainedFragment.mCurrCashback.getCustomer();

        } catch (BackendlessException e) {
            mRetainedFragment.mCurrCashback = null;
            mRetainedFragment.mCurrCustomer = null;

            LogMy.e(TAG, "Exception in getCashback: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int commitCashTrans(String pin) {
        return MerchantUser.getInstance().commitTxn(mRetainedFragment.mCurrTransaction, pin);
    }

    private int updateMerchantSettings() {
        return MerchantUser.getInstance().updateSettings();
    }

    private int logoutMerchant() {
        return MerchantUser.logoutSync();
    }

    private int fetchTransactions(String query) {
        mRetainedFragment.mLastFetchTransactions = null;
        int errorCode = ErrorCodes.NO_ERROR;

        mRetainedFragment.mLastFetchTransactions = MyTransaction.fetch(query);

        if(mRetainedFragment.mLastFetchTransactions == null) {
            errorCode = ErrorCodes.GENERAL_ERROR;
        } else if(mRetainedFragment.mLastFetchTransactions.size()==0) {
            errorCode = ErrorCodes.NO_DATA_FOUND;
        }
        return errorCode;
    }

    private int fetchTxnFiles(Context ctxt) {
        int errorCode = ErrorCodes.NO_ERROR;

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
            /*
            String filepath = missingFiles.get(i);
            String fileURL = BackendSettings.BACKEND_FILE_BASE_URL + filepath;
            //String filename = filepath.substring(filepath.lastIndexOf('/')+1);
            String filename = Uri.parse(fileURL).getLastPathSegment();
            LogMy.d(TAG,"Fetching "+fileURL+", Filename: "+filename);

            FileOutputStream outputStream;
            try {
                byte[] bitmapBytes = new FileFetchr().getUrlBytes(fileURL, MerchantUser.getInstance().getUserToken());

                outputStream = ctxt.openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(bitmapBytes);
                outputStream.close();

                //remove from missing files list
                mRetainedFragment.mMissingFiles.remove(missingFiles.get(i));

            } catch(FileNotFoundException fnf) {
                LogMy.d(TAG, "File not found: "+fnf.toString());
                errorCode = ErrorCodes.FILE_NOT_FOUND;
            } catch(IOException ioe) {
                LogMy.e(TAG, "Failed to fetch file: "+ioe.toString());
                errorCode = ErrorCodes.GENERAL_ERROR;
            }*/
        }

        return errorCode;
    }

    private int downloadFile(MessageFileDownload msg) {
        String filepath = msg.fileUrl;
        String fileURL = BackendSettings.BACKEND_FILE_BASE_URL + filepath;
        //String filename = filepath.substring(filepath.lastIndexOf('/')+1);
        String filename = Uri.parse(fileURL).getLastPathSegment();
        LogMy.d(TAG,"Fetching "+fileURL+", Filename: "+filename);

        FileOutputStream outputStream;
        try {
            byte[] bytes = new FileFetchr().getUrlBytes(fileURL, mRetainedFragment.mUserToken);

            outputStream = msg.ctxt.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(bytes);
            outputStream.close();

        } catch(FileNotFoundException fnf) {
            LogMy.d(TAG, "File not found: "+fnf.toString());
            return ErrorCodes.FILE_NOT_FOUND;
        } catch(IOException ioe) {
            LogMy.e(TAG, "Failed to fetch file: "+ioe.toString());
            return ErrorCodes.GENERAL_ERROR;
        }
        return ErrorCodes.NO_ERROR;
    }
}
