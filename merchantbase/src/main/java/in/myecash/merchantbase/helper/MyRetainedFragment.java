package in.myecash.merchantbase.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;

import com.crashlytics.android.Crashlytics;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.TxnReportsHelper;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantOps;
import in.myecash.common.database.MerchantOrders;
import in.myecash.common.database.MerchantStats;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.BackgroundProcessor;
import in.myecash.appbase.utilities.FileFetchr;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.RetainedFragment;
import in.myecash.merchantbase.entities.MyCustomerOps;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.MyCustomer;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.merchantbase.entities.OrderItem;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by adgangwa on 02-03-2016.
 */
public class MyRetainedFragment extends RetainedFragment {
    private static final String TAG = "MchntApp-MyRetainedFragment";

    // Requests that this fragment executes in backend
    public static final int REQUEST_IMAGE_DOWNLOAD = 0;
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
    public static final int REQUEST_UPLOAD_IMG = 17;
    public static final int REQUEST_ARCHIVE_TXNS = 18;
    public static final int REQUEST_CUST_DATA_FILE_DOWNLOAD = 19;
    public static final int REQUEST_FETCH_MERCHANT_OPS = 20;
    public static final int REQUEST_CANCEL_TXN = 21;
    public static final int REQUEST_CRT_MCHNT_ORDER = 22;
    public static final int REQUEST_FETCH_MERCHANT_ORDERS = 23;
    public static final int REQUEST_DELETE_MCHNT_ORDER = 24;
    public static final int REQUEST_GEN_TXN_OTP = 25;
    public static final int REQUEST_LOAD_TEST = 26;

    // Threads taken care by this fragment
    private MyBackgroundProcessor<String> mBackgroundProcessor;
    private FetchImageTask mFetchImageTask;

    public String mUserToken;
    public MerchantUser mMerchantUser;
    // Current objects - should be reset after each transaction
    public String mCustMobile;
    public String mCustCardId;
    public boolean mCardPresented;
    public String mCardImageFilename;

    public String mCustRegFirstName;
    public String mCustRegLastName;
    public String mCustRegDob; // DDMMYYYY format
    public int mCustSex;

    public MyCashback mCurrCashback;
    public MyCustomer mCurrCustomer;
    public MyTransaction mCurrTransaction;
    public MerchantStats mMerchantStats;
    public List<MyCashback> mLastFetchCashbacks;
    public List<MerchantOps> mLastFetchMchntOps;
    public Bitmap mLastFetchedImage;
    public List<MerchantOrders> mLastFetchMchntOrders;

    public int mBillTotal;
    // mCbExcludedTotal is not used - should always be 0
    public int mCbExcludedTotal;
    // mOrderItems is not used - to be removed
    public List<OrderItem> mOrderItems;
    public int toDeleteTrustedDeviceIndex = -1;

    public MyCustomerOps mCustomerOp;
    // params for merchant mobile number change operation
    public String mVerifyParamMobileChange;
    public String mNewMobileNum;
    public String mOtpMobileChange;

    // members used by 'Txn Reports Activity' to store its state, and its fragments
    //public List<String> mAllFiles = new ArrayList<>();
    public List<String> mMissingFiles;
    // 'Txn Reports Activity' store the helper instance here in onSaveInstance
    public TxnReportsHelper mTxnReportHelper;
    //public List<Transaction> mTxnsFromCsv = new ArrayList<>();
    public int mSummary[] = new int[AppConstants.INDEX_SUMMARY_MAX_VALUE];
    public List<Transaction> mLastFetchTransactions;

    public void reset() {
        LogMy.d(TAG,"In reset");
        mCurrCashback = null;
        mCurrTransaction = null;
        mCurrCustomer = null;

        mLastFetchTransactions = null;
        mLastFetchedImage = null;
        mLastFetchMchntOps = null;
        mLastFetchMchntOrders = null;

        mCustMobile = null;
        mCustCardId = null;
        mCardImageFilename = null;
        mCardPresented = false;
        toDeleteTrustedDeviceIndex = -1;

        mOrderItems = null;
        mCbExcludedTotal = 0;
        mBillTotal = 0;

        mCustomerOp= null;
        mVerifyParamMobileChange = null;
        mNewMobileNum = null;
        mOtpMobileChange = null;

        mCustRegFirstName = null;
        mCustRegLastName = null;
        mCustRegDob = null;
        mCustSex = -1;

        if(AppConstants.USE_CRASHLYTICS) {
            Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_MOBILE, "");
            Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_CARD, "");
        }
    }

    public void fetchMerchantsOps() {
        mBackgroundProcessor.addMerchantOpsReq();
    }

    public void archiveTxns() {mBackgroundProcessor.addArchiveTxnsRequest();}

    public void fetchMerchantStats() {
        mBackgroundProcessor.addMerchantStatsRequest();
    }

    public void changeMobileNum() {
        mBackgroundProcessor.addChangeMobileRequest();
    }

    public void deleteDevice(String curDeviceId) {
        mBackgroundProcessor.addDeleteDeviceRequest(toDeleteTrustedDeviceIndex, curDeviceId);
    }

    public void uploadImageFile(Context ctxt, String localStoredFileName, String remoteFileName, String remoteDir) {
        // get file object for the stored file
        File txnImage = new File(ctxt.getFilesDir() + "/" + localStoredFileName);
        // check if image of card exists
        if(txnImage.exists()) {
            // rename the file
            File to = new File(txnImage.getParentFile(), remoteFileName);
            if(txnImage.renameTo(to)) {
                // add upload request
                mBackgroundProcessor.addImgUploadRequest(to, remoteDir);
            } else {
                LogMy.w(TAG, "Txn Image file rename failed: "+txnImage.getAbsolutePath());
                ctxt.deleteFile(localStoredFileName);
                //raise alarm
                Map<String,String> params = new HashMap<>();
                params.put("FromFilePath",txnImage.getAbsolutePath());
                params.put("ToFilePath",to.getAbsolutePath());
                AppAlarms.localOpFailed(MerchantUser.getInstance().getMerchantId(), DbConstants.USER_TYPE_MERCHANT,"uploadImage",params);
            }

        } else {
            // for some reason file does not exist
            LogMy.w(TAG,"Image file does not exist: "+txnImage.getAbsolutePath());
            //raise alarm
            Map<String,String> params = new HashMap<>();
            params.put("FilePath",txnImage.getAbsolutePath());
            AppAlarms.localOpFailed(MerchantUser.getInstance().getMerchantId(),DbConstants.USER_TYPE_MERCHANT,"uploadImage",params);
        }
    }

    /*public void uploadImageFile(File file, String remoteDir) {
        mBackgroundProcessor.addImgUploadRequest(file, remoteDir);
    }*/

    public void changePassword(String oldPasswd, String newPasswd) {
        mBackgroundProcessor.changePassword(oldPasswd, newPasswd);
    }

    public void executeCustomerOp() {
        mBackgroundProcessor.addCustomerOp();
    }

    public void forgotId(String mobileNum, String deviceId) {
        mBackgroundProcessor.addForgotIdRequest(mobileNum, deviceId);
    }

    public void generatePassword(String dob, String deviceId, String userId) {
        mBackgroundProcessor.addPasswordRequest(dob, deviceId, userId);
    }

    public void fetchTxnFiles(Context context) {
        mBackgroundProcessor.addFetchTxnFilesRequest(context);
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

    public void registerCustomer(String mobileNum, String dob, int sex, String qrCode, String otp, String firstName, String lastName) {
        mBackgroundProcessor.addCustRegRequest(mobileNum, dob, sex, qrCode, otp, firstName, lastName);
    }

    public void commitCashTransaction(String pin, boolean isOtp) {
        mBackgroundProcessor.addCommitTransRequest(pin, isOtp);
    }

    public void downloadCustDataFile(Context ctxt, String fileURL) {
        mBackgroundProcessor.addCustFileDownloadReq(ctxt, fileURL);
    }

    public void fetchImageFile(String url) {
        LogMy.d(TAG, "In fetchImageFile: "+url);
        // most probably this will get called before OnActivityCreated and mMerchantUser will be null then
        mMerchantUser = MerchantUser.getInstance();

        // start new thread if old thread already running and not finished
        if( (mFetchImageTask!=null && mFetchImageTask.getStatus()== FetchImageTask.Status.FINISHED) ||
                mFetchImageTask == null) {
            mLastFetchedImage = null;
            mFetchImageTask = new FetchImageTask();
            mFetchImageTask.execute(url, mUserToken);
        }
    }

    public void cancelTxn(String txnId, String cardId, String pin, boolean isOtp) {
        mBackgroundProcessor.addCancelTxnReq(txnId, cardId, pin, isOtp);
    }

    public void generateTxnOtp(String custMobileOrId) {
        mBackgroundProcessor.addGenTxnOtpReq(custMobileOrId);
    }

    public void createMchntOrder(String sku, int qty, int totalPrice) {
        mBackgroundProcessor.createMchntOrder(sku, qty, totalPrice);
    }
    public void fetchMchntOrders() {
        mBackgroundProcessor.addFetchMchntOrderReq();
    }
    public void deleteMchntOrder(String orderId) {
        mBackgroundProcessor.addDeleteMchntOrder(orderId);
    }

    public void startLoadTest(String custId, String pin, int reps) {
        mBackgroundProcessor.addLoadTestReq(custId, pin, reps);
    }


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
        mBackgroundProcessor = null;
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
    private class FetchImageTask extends AsyncTask<String,Void,Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                /*
                String url = CommonConstants.BACKEND_FILE_BASE_URL+
                        CommonConstants.MERCHANT_DISPLAY_IMAGES_DIR+
                        mMerchantUser.getMerchant().getDisplayImage();
                byte[] bitmapBytes = new FileFetchr().getUrlBytes(url,
                        MerchantUser.getInstance().getUserToken());*/

                byte[] bitmapBytes = new FileFetchr().getUrlBytes(params[0],params[1]);
                return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

            } catch (Exception ioe) {
                LogMy.e(TAG, "Failed to fetch image"+ ioe.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            if(image==null) {
                mCallback.onBgProcessResponse(ErrorCodes.GENERAL_ERROR, REQUEST_IMAGE_DOWNLOAD);
            } else {
                mLastFetchedImage = image;
                //mMerchantUser.setDisplayImage(image);
                mCallback.onBgProcessResponse(ErrorCodes.NO_ERROR, REQUEST_IMAGE_DOWNLOAD);
            }
        }
    }
}