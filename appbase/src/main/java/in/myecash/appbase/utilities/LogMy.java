package in.myecash.appbase.utilities;


import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Created by adgangwa on 30-08-2016.
 */
public class LogMy {
    public static void d(String tag, String msg) {
        Log.d(tag,msg);
        Crashlytics.log(msg);
    }
    public static void i(String tag, String msg) {
        Log.i(tag,msg);
        Crashlytics.log(msg);
    }
    public static void w(String tag, String msg) {
        Log.w(tag,msg);
        Crashlytics.log(msg);
    }
    public static void e(String tag, String msg) {
        Log.e(tag,msg);
        Crashlytics.log(msg);
    }
    public static void e(String tag, String msg, Exception e) {
        Log.e(tag,msg,e);
        Crashlytics.log(msg);
        Crashlytics.logException(e);
    }
    public static void e(String tag, String msg, Throwable t) {
        Log.e(tag,msg,t);
        Crashlytics.log(msg);
    }
    public static void wtf(String tag, String msg) {
        Log.wtf(tag,msg);
        Crashlytics.log(msg);
    }
}
