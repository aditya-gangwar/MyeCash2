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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.MenuItem;
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
import in.myecash.common.database.CustomerOps;
import in.myecash.common.database.Customers;
import in.myecash.common.database.MerchantDevice;
import in.myecash.common.database.MerchantOps;
import in.myecash.common.database.Merchants;
import in.myecash.common.database.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 16-02-2016.
 */
public class AppCommonUtil {
    private static final String TAG = "BaseApp-AppCommonUtil";

    private static final int FILE_READ_BLOCK_BYTES = 1024;

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
                if(mProgressDialog!=null) {
                    float small = context.getResources().getDimension(R.dimen.text_size_small);
                    TextView textView = (TextView) mProgressDialog.findViewById(android.R.id.message);
                    if (textView != null)
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, small);
                }
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
        String expMsg;

        if( e.getCode().equals("0") && e.getMessage().startsWith(CommonConstants.PREFIX_ERROR_CODE_AS_MSG) ) {
            LogMy.d(TAG,"Custom error code case: Orig: "+e.getCode()+","+e.getMessage());
            String[] csvFields = e.getMessage().split(CommonConstants.SPECIAL_DELIMETER, -1);
            expCode = csvFields[1];
            expMsg = csvFields[2];
        } else {
            expCode = e.getCode();
            expMsg = e.getMessage();
        }

        int errorCode;
        try {
            errorCode = Integer.parseInt(expCode);
        } catch(Exception et) {
            if( expMsg!=null && (expMsg.contains(CommonConstants.BACKENDLESS_HOST_IP)||expMsg.contains("Connection refused")) ) {
                LogMy.d(TAG,"Exiting getLocalErrorCode: "+ErrorCodes.REMOTE_SERVICE_NOT_AVAILABLE);
                return ErrorCodes.REMOTE_SERVICE_NOT_AVAILABLE;
            }
            LogMy.e(TAG,"Non-integer error code: "+expCode,e);
            return ErrorCodes.GENERAL_ERROR;
        }

        // Check if its defined error code
        // converting code to msg to check for it
        //String errMsg = AppCommonUtil.getErrorDesc(errorCode);
        if(!isLocalDefError(errorCode)) {
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

            // for some Special cases - some more info (integer) is sent by backend as exception msg
            // Extract and append that info in error code
            int newErrorCode = transformErrorCode(errorCode, expMsg);

            LogMy.d(TAG,"Exiting getLocalErrorCode: "+newErrorCode+", "+errorCode);
            return newErrorCode;
        }
    }

    private static boolean isLocalDefError(int errorCode) {
        return (ErrorCodes.appErrorDesc.get(errorCode)!=null);
    }

    public static String getErrorDesc(int errorCode) {
        LogMy.d(TAG,"In getErrorDesc: "+mUserType+", "+errorCode);

        String moreInfo = null;
        if(errorCode  > ErrorCodes.ERROR_MAX_CNT) {
            // some extra info is appended as end digits - only first 3 digits represent error code
            String code = String.valueOf(errorCode);
            moreInfo = code.substring(ErrorCodes.ERROR_DIGITS);
            errorCode = Integer.parseInt(code.substring(0,ErrorCodes.ERROR_DIGITS));
            LogMy.d(TAG,"Error code with extra info: "+errorCode+", "+moreInfo);
        }

        // handle all error messages requiring substitution seperatly
        switch(errorCode) {
            case ErrorCodes.FAILED_ATTEMPT_LIMIT_RCHD:
                return String.format(ErrorCodes.appErrorDesc.get(errorCode),Integer.toString(MyGlobalSettings.getAccBlockMins(mUserType)));

            case ErrorCodes.CASH_ACCOUNT_LIMIT_RCHD:
                return String.format(ErrorCodes.appErrorDesc.get(errorCode),Integer.toString(MyGlobalSettings.getCashAccLimit()));

            case ErrorCodes.WRONG_PIN:
            case ErrorCodes.VERIFICATION_FAILED_CARDID:
            case ErrorCodes.VERIFICATION_FAILED_PASSWD:
            case ErrorCodes.VERIFICATION_FAILED_DOB:
            case ErrorCodes.VERIFICATION_FAILED_MOBILE:
            case ErrorCodes.USER_WRONG_ID_PASSWD:
                int attempts;
                if(moreInfo==null) {
                    attempts = MyGlobalSettings.getWrongAttemptLimit(mUserType);
                } else {
                    attempts = Integer.parseInt(moreInfo);
                }
                //int confMaxAttempts = MyGlobalSettings.getWrongAttemptLimit(mUserType);
                //int availableAttempts = Integer.parseInt();
                return String.format(ErrorCodes.appErrorDesc.get(errorCode),String.valueOf(attempts));

            default:
                return ErrorCodes.appErrorDesc.get(errorCode);
        }
    }

    private static int transformErrorCode(int errorCode, String errMsg) {
        LogMy.d(TAG,"In transformErrorCode: "+mUserType+", "+errorCode+", "+errMsg);
        // handle all error messages requiring substitution seperatly
        switch(errorCode) {
            case ErrorCodes.WRONG_PIN:
            case ErrorCodes.VERIFICATION_FAILED_CARDID:
            case ErrorCodes.VERIFICATION_FAILED_PASSWD:
            case ErrorCodes.VERIFICATION_FAILED_DOB:
            case ErrorCodes.VERIFICATION_FAILED_MOBILE:
            case ErrorCodes.USER_WRONG_ID_PASSWD:
                try {
                    int info = Integer.parseInt(errMsg); //just to check if msg is valid integer
                    String newCode = String.valueOf(errorCode).concat(errMsg);
                    return Integer.parseInt(newCode);
                } catch(Exception e) {
                    // ignore
                    return errorCode;
                }

            default:
                return errorCode;
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
    /*public static String getMacAddress(Context context) {
        WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        String m_szWLANMAC = wm.getConnectionInfo().getMacAddress();
        return m_szWLANMAC;
        //return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }*/
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

    public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int colorResId) {
        Drawable normalDrawable = item.getIcon();
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(context, colorResId));

        item.setIcon(wrapDrawable);
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
        //File filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File filesDir = context.getFilesDir();
        if (filesDir == null) {
            return null;
        }

        return new File(filesDir, name);
    }

    public static void delAllInternalFiles(Activity ctxt) {
        String[] files = ctxt.fileList();
        for (String fileName :
                files) {
            if(AppCommonUtil.isAppFile(fileName)) {
                if (ctxt.deleteFile(fileName)) {
                    LogMy.d(TAG, "Deleted file: " + fileName);
                } else {
                    LogMy.e(TAG, "Failed to delete file: " + fileName);
                }
            }
        }
    }

    public static boolean isAppFile(String fileName) {
        return ( fileName.startsWith(CommonConstants.MERCHANT_TXN_FILE_PREFIX) ||
                fileName.startsWith(CommonConstants.MERCHANT_CUST_DATA_FILE_PREFIX) ||
                fileName.startsWith(CommonConstants.CASHBACK_DATA_FILE_PREFIX)||
                fileName.startsWith(CommonConstants.PREFIX_TXN_IMG_FILE_NAME) );
    }

    public static byte[] fileAsByteArray(Context ctxt, String fileName) throws Exception {

        FileInputStream inputStream = null;
        try {
            inputStream = ctxt.openFileInput(fileName);

            byte[] b = new byte[FILE_READ_BLOCK_BYTES];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = inputStream.read(b)) != -1) {
                os.write(b, 0, c);
            }
            return os.toByteArray();
        } finally {
            if(inputStream!=null) {
                inputStream.close();
            }
        }
    }

    public static int loadGlobalSettings(MyGlobalSettings.RunMode runMode) {
        try {
            MyGlobalSettings.initSync(runMode);
            return checkServiceTime();

        } catch (BackendlessException e) {
            LogMy.e(TAG,"Failed to fetch global settings: "+e.toString());
            AppAlarms.handleException(e);
            return AppCommonUtil.getLocalErrorCode(e);
        }
    }

    private static int checkServiceTime() {
        // Check for daily downtime
        int startHour = MyGlobalSettings.getDailyDownStartHour();
        int endHour = MyGlobalSettings.getDailyDownEndHour();
        if(endHour > startHour) {
            int currHour = (new DateUtil()).getHourOfDay();
            if(currHour >= startHour && currHour < endHour) {
                return ErrorCodes.UNDER_DAILY_DOWNTIME;
            }
        }
        // Check maintenance window time
        Date disabledUntil = MyGlobalSettings.getServiceDisabledUntil();
        if(disabledUntil == null || System.currentTimeMillis() > disabledUntil.getTime()) {
            return ErrorCodes.NO_ERROR;
        } else {
            return ErrorCodes.SERVICE_GLOBAL_DISABLED;
        }
    }

    public static String isDownAsPerLocalData(Context ctxt) {
        // check daily downtime as stored locally
        int startHour = PreferenceManager.getDefaultSharedPreferences(ctxt)
                .getInt(AppConstants.PREF_DAILY_DWNTIME_START_HOUR, 0);
        int endHour = PreferenceManager.getDefaultSharedPreferences(ctxt)
                .getInt(AppConstants.PREF_DAILY_DWNTIME_END_HOUR, 0);
        if(endHour > startHour) {
            int currHour = (new DateUtil()).getHourOfDay();
            if(currHour >= startHour && currHour < endHour) {
                return String.format(AppCommonUtil.getErrorDesc(ErrorCodes.UNDER_DAILY_DOWNTIME),
                        startHour,
                        endHour);
            }
        }
        return null;
    }

    public static void storeGSLocally(Context ctxt) {
        // update Global Settings which need to be stored locally
        PreferenceManager.getDefaultSharedPreferences(ctxt)
                .edit()
                .putInt(AppConstants.PREF_DAILY_DWNTIME_START_HOUR, MyGlobalSettings.getDailyDownStartHour())
                .apply();
        PreferenceManager.getDefaultSharedPreferences(ctxt)
                .edit()
                .putInt(AppConstants.PREF_DAILY_DWNTIME_END_HOUR, MyGlobalSettings.getDailyDownEndHour())
                .apply();
        PreferenceManager.getDefaultSharedPreferences(ctxt)
                .edit()
                .putString(AppConstants.PREF_SERVICE_NA_URL, MyGlobalSettings.getServiceNAUrl())
                .apply();
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
        Backendless.Data.mapTableToClass("CustomerOps", CustomerOps.class);
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

