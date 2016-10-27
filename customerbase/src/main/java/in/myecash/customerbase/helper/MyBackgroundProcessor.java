package in.myecash.customerbase.helper;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.backendless.exceptions.BackendlessException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import in.myecash.appbase.entities.MyCashback;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.FileFetchr;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 17-07-2016.
 */
public class MyBackgroundProcessor <T> extends BackgroundProcessor<T> {
    private final static String TAG = "MyBackgroundProcessor";

    private MyRetainedFragment mRetainedFragment;

    public MyBackgroundProcessor(Handler responseHandler, MyRetainedFragment retainedFragment) {
        super(responseHandler);
        mRetainedFragment = retainedFragment;
    }

    private class MessageLogin implements Serializable {
        public String userId;
        public String password;
    }
    private class MessageChangePassword implements Serializable {
        public String oldPasswd;
        public String newPasswd;
    }
    private class MessageChangePin implements Serializable {
        public String oldPin;
        public String newPin;
        public String cardNum;
    }
    private class MessageGetCb implements Serializable {
        public Context ctxt;
        public Long updatedSince;
    }
    private class MessageFileDownload implements Serializable {
        public Context ctxt;
        public String fileUrl;
    }

    /*
     * Add request methods
     */
    public void addLoginRequest(String userId, String password) {
        MessageLogin msg = new MessageLogin();
        msg.userId = userId;
        msg.password = password;
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
    public void addChangeMobileRequest() {
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_MOBILE,null).sendToTarget();
    }
    public void addFetchCbRequest(Long updatedSince, Context ctxt) {
        MessageGetCb msg = new MessageGetCb();
        msg.ctxt = ctxt;
        msg.updatedSince = updatedSince;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_CB,msg).sendToTarget();
    }
    public void addPinChangeRequest(String oldPin, String newPin, String cardNum) {
        MessageChangePin msg = new MessageChangePin();
        msg.oldPin = oldPin;
        msg.newPin = newPin;
        msg.cardNum = cardNum;
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_CHANGE_PIN,msg).sendToTarget();
    }
    public void addFetchTxnsRequest(String query) {
        LogMy.d(TAG, "In addFetchTxnsRequest");
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_TXNS, query).sendToTarget();
    }
    public void addFetchTxnFilesRequest(Context context, List<String> missingFiles) {
        LogMy.d(TAG, "In addFetchTxnFilesRequest: " + missingFiles.size());
        mRequestHandler.obtainMessage(MyRetainedFragment.REQUEST_FETCH_TXN_FILES, context).sendToTarget();
    }


    @Override
    protected int handleMsg(Message msg) {
        int error = ErrorCodes.NO_ERROR;
        switch(msg.what) {
            case MyRetainedFragment.REQUEST_LOGIN:
                error = loginCustomer((MessageLogin) msg.obj);
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
            case MyRetainedFragment.REQUEST_CHANGE_MOBILE:
                error = changeMobileNum();
                break;
            case MyRetainedFragment.REQUEST_FETCH_CB:
                error = fetchCashbacks((MessageGetCb) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_CHANGE_PIN:
                error = changePin((MessageChangePin) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_FETCH_TXNS:
                error = fetchTransactions((String) msg.obj);
                break;
            case MyRetainedFragment.REQUEST_FETCH_TXN_FILES:
                error = fetchTxnFiles((Context) msg.obj);
                break;
        }
        return error;
    }

    private int loginCustomer(MessageLogin msg) {
        LogMy.d(TAG, "In loginMerchant");
        return CustomerUser.login(msg.userId, msg.password);
    }

    private int logoutAgent() {
        return CustomerUser.logout();
    }

    private int generatePassword(MessageLogin msg) {
        return CustomerUser.resetPassword(msg.userId, msg.password);
    }

    private int changePassword(MessageChangePassword msg) {
        return CustomerUser.getInstance().changePassword(msg.oldPasswd, msg.newPasswd);
    }

    private int changeMobileNum() {
        return CustomerUser.getInstance().changeMobileNum(mRetainedFragment.mPinMobileChange,
                mRetainedFragment.mNewMobileNum, mRetainedFragment.mOtpMobileChange);
    }

    private int fetchCashbacks(MessageGetCb msg) {
        mRetainedFragment.mLastFetchCashbacks = null;
        try {
            List<Cashback> cashbacks = CustomerUser.getInstance().fetchCashbacks(msg.updatedSince);

            if(cashbacks.size() > 0) {
                mRetainedFragment.mLastFetchCashbacks = new ArrayList<>(cashbacks.size());
                for (Cashback cb :
                        cashbacks) {
                    MyCashback myCb = new MyCashback();
                    myCb.init(cb, false);
                    mRetainedFragment.mLastFetchCashbacks.add(myCb);
                }
            }

            // fetch mchnt DPs
            // ignore any error
            try {
                fetchMchntDpFiles(msg.ctxt);
            } catch(Exception ex) {
                LogMy.e(TAG,"Exception from fetchMchntDpFiles",ex);
            }

        } catch (BackendlessException e) {
            mRetainedFragment.mLastFetchCashbacks = null;
            LogMy.e(TAG, "Exception in fetchCashbacks: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    private int fetchMchntDpFiles(Context ctxt) {
        int errorCode = ErrorCodes.NO_ERROR;

        // check which files need to be fetched
        // i.e. not present locally
        List<String> missingFiles = null;
        for (MyCashback myCb :
                mRetainedFragment.mLastFetchCashbacks) {
            String dpFilename = myCb.getMerchant().getDpFilename();

            File file = ctxt.getFileStreamPath(dpFilename);
            if(file == null || !file.exists()) {
                if(missingFiles==null) {
                    missingFiles = new ArrayList<>(mRetainedFragment.mLastFetchCashbacks.size());
                }
                // file does not exist
                LogMy.d(TAG,"Missing mchnt dp file: "+dpFilename);
                String filepath = CommonConstants.MERCHANT_DISPLAY_IMAGES_DIR + dpFilename;
                missingFiles.add(filepath);
            }
        }

        if(missingFiles!=null) {
            MessageFileDownload msg = new MessageFileDownload();
            for(int i=0; i<missingFiles.size(); i++) {
                msg.ctxt = ctxt;
                msg.fileUrl = missingFiles.get(i);
                errorCode = downloadFile(msg);
                //remove from missing files list
                if(errorCode==ErrorCodes.NO_ERROR) {
                    LogMy.d(TAG,"Downloaded mchnt dp file: "+missingFiles.get(i));
                }
            }
        }

        return errorCode;
    }

    private int changePin(MessageChangePin msg) {
        return CustomerUser.getInstance().changePin(msg.oldPin, msg.newPin, msg.cardNum);
    }

    private int fetchTransactions(String query) {
        mRetainedFragment.mLastFetchTransactions = null;
        int errorCode = ErrorCodes.NO_ERROR;

        try {
            List<Transaction> txns = CustomerUser.getInstance().fetchTxns(query);
            if(txns!=null && txns.size() > 0) {
                mRetainedFragment.mLastFetchTransactions = txns;
            } else {
                errorCode = ErrorCodes.NO_DATA_FOUND;
            }
        } catch (BackendlessException e) {
            mRetainedFragment.mLastFetchCashbacks = null;
            LogMy.e(TAG, "Exception in fetchTransactions: "+ e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
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
        }

        return errorCode;
    }

    private int downloadFile(MessageFileDownload msg) {
        String filepath = msg.fileUrl;
        String fileURL = CommonConstants.BACKEND_FILE_BASE_URL + filepath;
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
