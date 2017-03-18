package in.myecash.appbase.utilities;


import android.util.Log;

import com.crashlytics.android.Crashlytics;

import in.myecash.appbase.constants.AppConstants;

/**
 * Created by adgangwa on 30-08-2016.
 */
public class LogMy {
    public static void d(String tag, String msg) {
        //if(CommonConstants.IS_PRODUCTION_RELEASE) return;
        if(AppConstants.DEBUG_MODE) {
            Log.d(tag,msg);
        }
        logCrashlytics(msg);
    }
    public static void i(String tag, String msg) {
        //if(CommonConstants.IS_PRODUCTION_RELEASE) return;
        if(AppConstants.DEBUG_MODE) {
            Log.i(tag,msg);
        }
        logCrashlytics(msg);
    }
    public static void w(String tag, String msg) {
        if(AppConstants.DEBUG_MODE) {
            Log.w(tag,msg);
        }
        logCrashlytics(msg);
    }
    public static void e(String tag, String msg) {
        if(AppConstants.DEBUG_MODE) {
            Log.e(tag,msg);
        }
        logCrashlytics(msg);
    }
    public static void e(String tag, String msg, Exception e) {
        if(AppConstants.DEBUG_MODE) {
            Log.e(tag,msg,e);
        }
        logCrashlytics(msg);
    }
    public static void e(String tag, String msg, Throwable t) {
        if(AppConstants.DEBUG_MODE) {
            Log.e(tag,msg,t);
        }
        logCrashlytics(msg);
    }
    public static void wtf(String tag, String msg) {
        if(AppConstants.DEBUG_MODE) {
            Log.wtf(tag,msg);
        }
        logCrashlytics(msg);
    }

    private static void logCrashlytics(String msg) {
        if(AppConstants.USE_CRASHLYTICS) {
            Crashlytics.log(msg);
        }
    }
}
