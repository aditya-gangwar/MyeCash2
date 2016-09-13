package in.myecash.merchantbase.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;

import com.crashlytics.android.Crashlytics;
import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.BackendSettings;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.MerchantStats;
import in.myecash.commonbase.models.Transaction;
import in.myecash.commonbase.utilities.BackgroundProcessor;
import in.myecash.commonbase.utilities.FileFetchr;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.RetainedFragment;
import in.myecash.merchantbase.entities.CustomerOps;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.entities.MyCashback;
import in.myecash.merchantbase.entities.MyCustomer;
import in.myecash.merchantbase.entities.MyTransaction;
import in.myecash.merchantbase.entities.OrderItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adgangwa on 02-03-2016.
 */
public class MyRetainedFragment extends RetainedFragment {
    private static final String TAG = "MyRetainedFragment";

    // Requests that this fragment executes in backend
    public static final int REQUEST_MCHNT_DP_DOWNLOAD = 0;
    public static final int REQUEST_REGISTER_MERCHANT = 1;
    public static final int REQUEST_GET_CASHBACK = 2;
    public static final int REQUEST_REGISTER_CUSTOMER = 3;
    public static final int REQUEST_LOGIN = 4;
    public static final int REQUEST_COMMIT_TRANS = 5;
    public static final int REQUEST_UPDATE_MERCHANT_SETTINGS = 6;
    public static final int REQUEST_LOGOUT_MERCHANT = 7;
    public static final int REQUEST_FETCH_TXNS = 8;
    public static final int REQUEST_FETCH_TXN_FILES = 9;
    public static final int REQUEST_GENERATE_MERCHANT_PWD = 10;
    public static final int REQUEST_ADD_CUSTOMER_OP = 11;
    public static final int REQUEST_CHANGE_PASSWD = 12;
    public static final int REQUEST_DELETE_TRUSTED_DEVICE = 13;
    public static final int REQUEST_CHANGE_MOBILE = 14;
    public static final int REQUEST_MERCHANT_STATS = 15;
    public static final int REQUEST_FORGOT_ID = 16;
    public static final int REQUEST_UPLOAD_FILE = 17;
    public static final int REQUEST_ARCHIVE_TXNS = 18;
    public static final int REQUEST_CUST_DATA_FILE_DOWNLOAD = 19;

    // Threads taken care by this fragment
    private MyBackgroundProcessor<String> mBackgroundProcessor;
    private FetchImageTask mFetchImageTask;

    public MerchantUser mMerchantUser;
    // Current objects - should be reset after each transaction
    public String mCustMobile;
    public String mCustCardId;
    public boolean mCardPresented;
    public String mCardImageFile;

    public MyCashback mCurrCashback;
    public MyCustomer mCurrCustomer;
    public MyTransaction mCurrTransaction;
    public MerchantStats mMerchantStats;
    public List<MyCashback> mLastFetchCashbacks;

    public int mBillTotal;
    public int mCbExcludedTotal;
    public List<OrderItem> mOrderItems;
    public int toDeleteTrustedDeviceIndex = -1;

    public CustomerOps mCustomerOp;
    // params for merchant mobile number change operation
    public String mInputCurrMobile;
    public String mNewMobileNum;
    public String mOtpMobileChange;

    // members used by 'Reports Activity' to store its state, and its fragments
    public List<String> mAllFiles = new ArrayList<>();
    public List<String> mMissingFiles = new ArrayList<>();
    public List<Transaction> mTxnsFromCsv = new ArrayList<>();
    public int mSummary[] = new int[AppConstants.INDEX_SUMMARY_MAX_VALUE];
    public List<Transaction> mLastFetchTransactions;

    public void reset() {
        LogMy.d(TAG,"In reset");
        mCurrCashback = null;
        mCurrTransaction = null;
        mCurrCustomer = null;
        mLastFetchTransactions = null;

        mCustMobile = null;
        mCustCardId = null;
        mCardImageFile = null;
        mCardPresented = false;
        toDeleteTrustedDeviceIndex = -1;

        mOrderItems = null;
        mCbExcludedTotal = 0;
        mBillTotal = 0;

        mCustomerOp= null;
        mInputCurrMobile = null;
        mNewMobileNum = null;
        mOtpMobileChange = null;

        Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_MOBILE, "");
        Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_CARD, "");
    }

    public void archiveTxns() {mBackgroundProcessor.addArchiveTxnsRequest();}

    public void fetchMerchantStats() {
        mBackgroundProcessor.addMerchantStatsRequest();
    }

    public void changeMobileNum() {
        mBackgroundProcessor.addChangeMobileRequest();
    }

    public void deleteDevice() {
        mBackgroundProcessor.addDeleteDeviceRequest(toDeleteTrustedDeviceIndex);
    }

    public void uploadTxnImageFile(File file) {
        mBackgroundProcessor.addTxnImgUploadRequest(file);
    }

    public void changePassword(String oldPasswd, String newPasswd) {
        mBackgroundProcessor.changePassword(oldPasswd, newPasswd);
    }

    public void executeCustomerOp() {
        mBackgroundProcessor.addCustomerOp();
    }

    public void forgotId(String mobileNum, String deviceId) {
        mBackgroundProcessor.addForgotIdRequest(mobileNum, deviceId);
    }

    public void generatePassword(String brandName, String deviceId, String userId) {
        mBackgroundProcessor.addPasswordRequest(brandName, deviceId, userId);
    }

    public void fetchTxnFiles(Context context,List<String> missingFiles) {
        mBackgroundProcessor.addFetchTxnFilesRequest(context, missingFiles);
    }

    public void fetchTransactions(String whereClause) {
        mBackgroundProcessor.addFetchTxnsRequest(whereClause);
    }

    public void loginUser(String userId, String password, String deviceId, String otp) {
        mBackgroundProcessor.addLoginRequest(userId, password, deviceId, otp);
    }

    public void logoutMerchant() {
        mBackgroundProcessor.addLogoutRequest();
    }

    public void updateMerchantSettings() {
        mBackgroundProcessor.addMerchantSettingsRequest();
    }

    public void fetchCashback(String custId) {
        mBackgroundProcessor.addCashbackRequest(custId);
    }

    public void registerCustomer(String name, String mobileNum, String qrCode) {
        mBackgroundProcessor.addCustRegRequest(name, mobileNum, qrCode);
    }

    public void commitCashTransaction(String pin) {
        //mBackgroundProcessor.addCommitTransRequest(mTransaction);
        mBackgroundProcessor.addCommitTransRequest(pin);
    }

    public void downloadCustDataFile(Context ctxt, String fileURL) {
        mBackgroundProcessor.addCustFileDownloadReq(ctxt, fileURL);
    }

    public void fetchMerchantDp() {
        LogMy.d(TAG, "In fetchMerchantDp");
        // most probably this will get called before OnActivityCreated and mMerchantUser will be null then
        mMerchantUser = MerchantUser.getInstance();
        // start new thread if old thread already running and not finished
        if( (mFetchImageTask!=null && mFetchImageTask.getStatus()== FetchImageTask.Status.FINISHED) ||
                mFetchImageTask == null) {
            mFetchImageTask = new FetchImageTask();
            mFetchImageTask.execute();
        }
    }

    /*
    public void registerMerchant(File displayImage) {
        LogMy.d(TAG, "In registerMerchant");
        // start new thread if old thread already running and not finished
        if( (mRegMerchantTask!=null && mRegMerchantTask.getStatus()==RegisterMerchantTask.Status.FINISHED) ||
                mRegMerchantTask == null) {
            mRegMerchantTask = new RegisterMerchantTask();
            mRegMerchantTask.execute(displayImage);
        }
    }*/

    @Override
    protected void doOnActivityCreated() {
        mMerchantUser = MerchantUser.getInstance();
    }

    @Override
    protected BackgroundProcessor<String> getBackgroundProcessor() {
        if(mBackgroundProcessor == null) {
            LogMy.d(TAG,"Creating background thread.");
            Handler responseHandler = new Handler();
            mBackgroundProcessor = new MyBackgroundProcessor<>(responseHandler, this);
        }
        return mBackgroundProcessor;
    }

    @Override
    protected void doOnDestroy() {
        if(mFetchImageTask!=null) {
            mFetchImageTask.cancel(true);
            mFetchImageTask = null;
        }
        /*
        if(mRegMerchantTask!=null) {
            mRegMerchantTask.cancel(true);
            mRegMerchantTask = null;
        }*/
    }

    // Async task to fetch merchant display image file
    private class FetchImageTask extends AsyncTask<Void,Void,Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                String url = BackendSettings.BACKEND_FILE_BASE_URL+
                        CommonConstants.MERCHANT_DISPLAY_IMAGES_DIR+
                        mMerchantUser.getMerchant().getDisplayImage();

                byte[] bitmapBytes = new FileFetchr().getUrlBytes(url,
                        MerchantUser.getInstance().getUserToken());
                return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            } catch (IOException ioe) {
                LogMy.e(TAG, "Failed to fetch image"+ ioe.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            if(image==null) {
                mCallback.onBgProcessResponse(ErrorCodes.GENERAL_ERROR, REQUEST_MCHNT_DP_DOWNLOAD);
            } else {
                mMerchantUser.setDisplayImage(image);
                mCallback.onBgProcessResponse(ErrorCodes.NO_ERROR, REQUEST_MCHNT_DP_DOWNLOAD);
            }
        }
    }

    /*
    private class RegisterMerchantTask extends AsyncTask<File,Void,Integer> {
        @Override
        protected Integer doInBackground(File... params) {
            int retValue = ErrorCodes.NO_ERROR;
            for (File imageFile : params) {
                // register irrespecive of image upload status
                retValue = mMerchantUser.register(imageFile);
                // first upload image, so as the 'image URL' can be set as merchant property
                // thus avoiding extra API call to save URL value
                //mMerchantUser.setUser_id(null);
                //mMerchantUser.getMerchant().setDisplayImage(mMerchantUser.uploadDisplayImage(imageFile));
            }
            return retValue;
        }

        @Override
        protected void onPostExecute(Integer errorCode) {
            mCallback.onBgProcessResponse(errorCode, REQUEST_REGISTER_MERCHANT);
        }
    }*/

}

                /*
                switch(operation) {
                    case BackgroundProcessor.MESSAGE_GET_CASHBACK:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_GET_CASHBACK);
                        break;
                    case BackgroundProcessor.MESSAGE_REGISTER_CUSTOMER:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_REGISTER_CUSTOMER);
                        break;
                    case BackgroundProcessor.MESSAGE_LOGIN:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_LOGIN);
                        break;
                    case BackgroundProcessor.MESSAGE_COMMIT_TRANS:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_COMMIT_TRANS);
                        break;
                    case BackgroundProcessor.MESSAGE_UPDATE_MERCHANT_SETTINGS:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_UPDATE_MERCHANT_SETTINGS);
                        break;
                    case BackgroundProcessor.MESSAGE_LOGOUT_MERCHANT:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_LOGOUT_MERCHANT);
                        break;
                    case BackgroundProcessor.MESSAGE_FETCH_TRANSACTIONS:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_FETCH_TXNS);
                        break;
                    case BackgroundProcessor.MESSAGE_FETCH_TXN_FILES:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_FETCH_TXN_FILES);
                        break;
                    case BackgroundProcessor.MESSAGE_GENERATE_PASSWORD:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_GENERATE_MERCHANT_PWD);
                        break;
                    case BackgroundProcessor.MESSAGE_CUSTOMER_OP:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_ADD_CUSTOMER_OP);
                        break;
                    case BackgroundProcessor.MESSAGE_CHANGE_PASSWD:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_CHANGE_PASSWD);
                        break;
                    case BackgroundProcessor.MESSAGE_UPLOAD_FILE:
                    case BackgroundProcessor.MESSAGE_UPDATE_CUSTOMER:
                        // do nothing
                        break;
                    case BackgroundProcessor.MESSAGE_DELETE_DEVICE:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_DELETE_TRUSTED_DEVICE);
                        break;
                    case BackgroundProcessor.MESSAGE_CHANGE_MOBILE:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_CHANGE_MOBILE);
                        break;
                    case BackgroundProcessor.MESSAGE_MERCHANT_STATS:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_MERCHANT_STATS);
                        break;
                    case BackgroundProcessor.MESSAGE_FORGOT_ID:
                        mCallback.onBgProcessResponse(errorCode, REQUEST_FORGOT_ID);
                        break;
                }*/
