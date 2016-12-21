package in.myecash.appbase.utilities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.Counters;
import com.backendless.exceptions.BackendlessException;

import in.myecash.appbase.R;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.DateUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.Address;
import in.myecash.common.database.BusinessCategories;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.Cities;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.Customers;
import in.myecash.common.database.MerchantDevice;
import in.myecash.common.database.MerchantOps;
import in.myecash.common.database.Merchants;
import in.myecash.common.database.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 16-02-2016.
 */
public class AppCommonUtil {
    private static final String TAG = "AndroidUtil";
    //private static final SimpleDateFormat mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);

    // single active progress dialog at any time
    private static Toast mToast;
    private static ProgressDialog mProgressDialog;
    private static String mProgressDialogMsg;

    private static int mUserType;
    public static void setUserType(int userType) {
        AppCommonUtil.mUserType = userType;
    }

    /*
         * Progress Dialog related fxs
         */
    public static void showProgressDialog(final Context context, String message) {
        cancelProgressDialog(true);
        mProgressDialogMsg = message;
        //mProgressDialog = new ProgressDialog(context, R.style.ProgressDialogCustom);
        mProgressDialog = new ProgressDialog(context);

        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(mProgressDialogMsg);
        // No way to cancel the progressDialog
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                float small = context.getResources().getDimension(R.dimen.text_size_small);
                TextView textView = (TextView) mProgressDialog.findViewById(android.R.id.message);
                if(textView!=null)
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, small);
            }
        });
        mProgressDialog.show();
    }
    public static void cancelProgressDialog(boolean taskOver) {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if(taskOver) {
            mProgressDialogMsg = null;
        }
    }
    public static String getProgressDialogMsg() {
        return mProgressDialogMsg;
    }

    /*
     8 Show toast on screen
     */
    public static void toast(Context context, String msg) {
        if(mToast!=null)
            mToast.cancel();
        mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }
    public static void cancelToast() {
        if(mToast!=null)
            mToast.cancel();
    }

    /*
     * Convert Edittext to view only
     */
    public static void makeEditTextOnlyView(EditText et) {
        et.setFocusable(false);
        et.setClickable(false);
        et.setCursorVisible(false);
        et.setInputType(EditorInfo.TYPE_NULL);
    }

    /*
     * Check internet availability
     */
    public static int isNetworkAvailableAndConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable &&  cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected? ErrorCodes.NO_ERROR:ErrorCodes.NO_INTERNET_CONNECTION;
    }

    /*
     * Fxs to hide onscreen keyboard
     */
    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    public static void hideKeyboard(Dialog dialog) {
        if (dialog != null && dialog.getWindow() != null && dialog.getWindow().getDecorView() != null) {
            InputMethodManager imm = (InputMethodManager)dialog.getOwnerActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(dialog.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /*
     * Error codes related
     */
    public static int getLocalErrorCode(BackendlessException e) {
        LogMy.d(TAG,"Entering getLocalErrorCode: "+e.getCode());
        String expCode;
        if( e.getCode().equals("0") && e.getMessage().startsWith(CommonConstants.PREFIX_ERROR_CODE_AS_MSG) ) {
            LogMy.d(TAG,"Custom error code case: Orig: "+e.getCode()+","+e.getMessage());
            String[] csvFields = e.getMessage().split("/", -1);
            expCode = csvFields[1];
        } else {
            expCode = e.getCode();
        }

        int errorCode;
        try {
            errorCode = Integer.parseInt(expCode);
        } catch(Exception et) {
            if(e.getMessage().contains(CommonConstants.BACKENDLESS_HOST_IP)) {
                LogMy.d(TAG,"Exiting getLocalErrorCode: "+ErrorCodes.REMOTE_SERVICE_NOT_AVAILABLE);
                return ErrorCodes.REMOTE_SERVICE_NOT_AVAILABLE;
            }
            LogMy.e(TAG,"Non-integer error code: "+expCode,e);
            return ErrorCodes.GENERAL_ERROR;
        }

        // Check if its defined error code
        // converting code to msg to check for it
        String errMsg = AppCommonUtil.getErrorDesc(errorCode);
        if(errMsg==null) {
            // may be this is backendless error code
            Integer status = ErrorCodes.backendToLocalErrorCode.get(expCode);
            if(status == null) {
                // its not backendless code
                // this is some not expected error code
                // as app will not be able to convert it into valid message description
                // so return generic error code instead
                // Also log the same for analysis
                AppAlarms.handleException(e);
                LogMy.d(TAG,"Exiting getLocalErrorCode: "+ErrorCodes.GENERAL_ERROR);
                return ErrorCodes.GENERAL_ERROR;
            } else {
                // its backendless code
                LogMy.d(TAG,"Exiting getLocalErrorCode: "+status);
                return status;
            }
        } else {
            // its locally defined error
            LogMy.d(TAG,"Exiting getLocalErrorCode: "+errorCode);
            return errorCode;
        }
    }
    public static String getErrorDesc(int errorCode) {
        LogMy.d(TAG,"In getErrorDesc: "+mUserType);
        // handle all error messages requiring substitution seperatly
        switch(errorCode) {
            case ErrorCodes.FAILED_ATTEMPT_LIMIT_RCHD:
                return String.format(ErrorCodes.appErrorDesc.get(errorCode),Integer.toString(MyGlobalSettings.getAccBlockHrs(mUserType)));

            case ErrorCodes.CASH_ACCOUNT_LIMIT_RCHD:
                return String.format(ErrorCodes.appErrorDesc.get(errorCode),Integer.toString(MyGlobalSettings.getCashAccLimit()));

            case ErrorCodes.WRONG_PIN:
            case ErrorCodes.VERIFICATION_FAILED:
            case ErrorCodes.USER_WRONG_ID_PASSWD:
                int confMaxAttempts = MyGlobalSettings.getWrongAttemptLimit(mUserType);
                return String.format(ErrorCodes.appErrorDesc.get(errorCode),String.valueOf(confMaxAttempts));

            default:
                return ErrorCodes.appErrorDesc.get(errorCode);
        }
    }

    /*
     * Image Processing functions
     */
    public static boolean createImageFromBitmap(Bitmap bmp, File to) {
        boolean status = true;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(to);

            // Here we Resize the Image ...
            //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bmp.compress(getImgCompressFormat(), 100,
                    fileOutputStream); // bm is the bitmap object
            //byte[] bsResized = byteArrayOutputStream.toByteArray();

            //fileOutputStream.write(bsResized);
            //fileOutputStream.close();

        } catch (Exception e) {
            status = false;
        } finally {
            try {
                fileOutputStream.close();
            } catch (Exception ignored) {}
        }

        return status;
    }

    public static boolean compressBmpAndStore(Context context, Bitmap bmp, String fileName) {
        FileOutputStream out = null;
        try {
            out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            //Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            bmp.compress(getImgCompressFormat(), 90, out);
            LogMy.d(TAG, "Compressed image to file: "+fileName);

        } catch (Exception e) {
            //e.printStackTrace();
            LogMy.e(TAG,"Exception in compressBmpAndStore",e);
            return false;
        } finally {
            try {
                out.close();
            } catch (Exception ignored) {}
        }
        return true;
    }

    public static Bitmap.CompressFormat getImgCompressFormat() {
        if(CommonConstants.PHOTO_FILE_FORMAT.equals("webp")) {
            return Bitmap.CompressFormat.WEBP;
        } else if(CommonConstants.PHOTO_FILE_FORMAT.equals("png")) {
            return Bitmap.CompressFormat.PNG;
        } else if(CommonConstants.PHOTO_FILE_FORMAT.equals("jpeg")) {
            return Bitmap.CompressFormat.JPEG;
        }
        LogMy.e(TAG,"Invalid image format: "+CommonConstants.PHOTO_FILE_FORMAT);
        return Bitmap.CompressFormat.JPEG;
    }

    public static Bitmap addDateTime(Context context, Bitmap bitmap) {
        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;

        Bitmap.Config config = bitmap.getConfig();
        if(config == null){
            config = Bitmap.Config.ARGB_8888;
        }
        //Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Bitmap newBitmap = bitmap.copy(config,true);

        Canvas canvas = new Canvas(newBitmap);
        // new antialised Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.rgb(61, 61, 61));
        // text size in pixels
        paint.setTextSize((int) (14 * scale));
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
        String gText = sdf.format(new Date());
        // draw text to the Canvas bottom centre
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        //int y = (bitmap.getHeight() + bounds.height())/2;

        canvas.drawText(gText, x, 0, paint);

        return newBitmap;
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        //int borderWidth = bitmap.getHeight()/10; //110th as border width
        //paint.setColor(borderColor);
        //canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getHeight()/2+borderWidth, paint);
        //canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getWidth() / 2, bitmap.getWidth() / 2, paint);
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    /*
     * Get Device Info
     */
    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        /*
        String deviceId = "";
        final TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephony.getDeviceId() != null) {
            deviceId = mTelephony.getDeviceId();
        } else {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceId;*/
    }
    public static String getIMEI(Context context) {
        final TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = mTelephony.getDeviceId();
        return (imei==null)?"":imei;
    }

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static Drawable getTintedDrawable(Context context, @DrawableRes int drawableResId, @ColorRes int colorResId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
        int color = ContextCompat.getColor(context, colorResId);
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    /*
     * Functions to add Rupee symbol to givcen Amount
     */
    public static String getSignedAmtStr(int value, boolean isCredit) {
        if(value==0) {
            return getAmtStr(value);
        }
        return isCredit ? "+ "+AppConstants.SYMBOL_RS +String.valueOf(value) : "- "+AppConstants.SYMBOL_RS +String.valueOf(value);
    }
    public static String getAmtStr(int value) {
        return AppConstants.SYMBOL_RS +String.valueOf(value);
    }
    public static String getAmtStr(String value) {
        return AppConstants.SYMBOL_RS +value;
    }
    // reverse of getAmtStr()
    public static int getValueAmtStr(String amtStr) {
        return Integer.parseInt(amtStr.replace(AppConstants.SYMBOL_RS,"").replace(" ",""));
    }
    // reverse of getSignedAmtStr()
    public static int getValueSignedAmtStr(String amtStr) {
        return Integer.parseInt(amtStr.replace(AppConstants.SYMBOL_RS,"").replace("+","").replace("-","").replace(" ",""));
    }

    /*
     * Fxs. to get Filename for various files
     */
    public static String getMerchantCustFileName(String merchantId) {
        // File name: customers_<merchant_id>.csv
        return CommonConstants.MERCHANT_CUST_DATA_FILE_PREFIX + merchantId + CommonConstants.CSV_FILE_EXT;
    }
    public static String getCashbackFileName(String userId) {
        // File name: customers_<user_id>.csv
        return CommonConstants.CASHBACK_DATA_FILE_PREFIX+userId+CommonConstants.CSV_FILE_EXT;
    }

    public static File createLocalImageFile(Context context, String name) {
        File filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File filesDir = context.getFilesDir();
        if (filesDir == null) {
            return null;
        }

        return new File(filesDir, name);
    }


    public static void setDialogTextSize(DialogFragment frag, AlertDialog dialog) {
        //int textSize = (int) Helper.getDimen(mainScreen, R.dimen.textSize12);
        float small = frag.getResources().getDimension(R.dimen.text_size_small);
        float medium = frag.getResources().getDimension(R.dimen.text_size_medium);
        LogMy.d(TAG, "Small = "+small+", Medium = "+medium);

        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        if(textView!=null)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, small);
        textView = (TextView) dialog.findViewById(android.R.id.title);
        if(textView!=null)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, medium);

        Button b = dialog.getButton(Dialog.BUTTON_POSITIVE);
        if(b!=null)
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, small);
        b = dialog.getButton(Dialog.BUTTON_NEGATIVE);
        if(b!=null)
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, small);
    }

    public static String getMchntRemovalDate(Date removeReqDate) {
        DateUtil reqTime = new DateUtil(removeReqDate);
        reqTime.addDays(MyGlobalSettings.getMchntExpiryDays());
        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);
        //return "Removal on "+sdf.format(reqTime.getTime());
        return sdf.format(reqTime.getTime());
    }

    public static Date getExpiryDate(Merchants merchant) {
        DateUtil renewDate = new DateUtil(merchant.getLastRenewDate());
        renewDate.addMonths(MyGlobalSettings.getMchntRenewalDuration());
        return renewDate.getTime();
    }
    public static Date getExpiryDate(Customers customer) {
        DateUtil renewDate = new DateUtil(customer.getLastRenewDate());
        renewDate.addMonths(MyGlobalSettings.getCustRenewalDuration());
        return renewDate.getTime();
    }

    public static void initTableToClassMappings() {
        Backendless.Data.mapTableToClass("CustomerCards", CustomerCards.class);
        Backendless.Data.mapTableToClass("Customers", Customers.class);
        Backendless.Data.mapTableToClass("Merchants", Merchants.class);
        Backendless.Data.mapTableToClass("Counters", Counters.class);
        Backendless.Data.mapTableToClass("MerchantOps", MerchantOps.class);
        Backendless.Data.mapTableToClass("MerchantDevice", MerchantDevice.class);
        Backendless.Data.mapTableToClass("BusinessCategories", BusinessCategories.class);
        Backendless.Data.mapTableToClass("Address", Address.class);
        Backendless.Data.mapTableToClass("Cities", Cities.class);

        Backendless.Data.mapTableToClass( "Transaction0", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback0", Cashback.class );

        Backendless.Data.mapTableToClass( "Transaction1", Transaction.class );
        Backendless.Data.mapTableToClass( "Cashback1", Cashback.class );

    }

}

    /*public static String getMerchantTxnDir(String merchantId) {
        // merchant directory: merchants/<first 3 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/
        return CommonConstants.MERCHANT_TXN_ROOT_DIR +
                merchantId.substring(0,3) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(0,5) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId;
    }
    public static String getMerchantCustFilePath(String merchantId) {
        // File name: customers_<merchant_id>.csv
        return CommonConstants.MERCHANT_CUST_DATA_ROOT_DIR +
                CommonConstants.MERCHANT_CUST_DATA_FILE_PREFIX+merchantId+CommonConstants.CSV_FILE_EXT;
    }
    public static String getTxnCsvFilename(Date date, String merchantId) {
        // File name: txns_<merchant_id>_<ddMMMyy>.csv
        return CommonConstants.MERCHANT_TXN_FILE_PREFIX + merchantId + "_" + mSdfOnlyDateFilename.format(date) + CommonConstants.CSV_FILE_EXT;
    }
    public static String getTxnImgDir(String merchantId) {
        // merchant directory: merchants/<first 3 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/
        return CommonConstants.MERCHANT_TXN_IMAGE_ROOT_DIR +
                merchantId.substring(0,3) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(0,5) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId;
    }*/

