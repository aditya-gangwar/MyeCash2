package in.myecash.appbase.utilities;

import com.backendless.exceptions.BackendlessException;
import com.crashlytics.android.Crashlytics;
import in.myecash.common.constants.DbConstants;

import java.util.Map;

/**
 * Created by adgangwa on 02-09-2016.
 */
public class AppAlarms {

    private static String ALARM_CSV_DELIM = ",";
    private static String ALARM_PARAM_DELIM = ":";

    // Alarm severity
    private static String ALARM_SEVERITY_FATAL = "Fatal";
    private static String ALARM_SEVERITY_ERROR = "Error";
    private static String ALARM_SEVERITY_WARN = "Warning";

    // Alarm group/classes
    private static String ALARM_CLASS_SECURITY = "classSecurity";
    private static String ALARM_CLASS_DATA_INTEGRITY = "classDataIntegrity";
    private static String ALARM_INVALID_STATE = "classInvalidState";
    private static String ALARM_IGNORE_IF_NOT_MANY = "classIgnoreIfNotMany";
    private static String ALARM_UPLOAD_FAILED = "classUploadFailed";

    // Alarm names/codes
    private static String ALARM_DEVICE_ROOTED = "deviceRooted";
    private static String ALARM_NO_DATA = "noDataAvailable";
    private static String ALARM_INVALID_CARD_STATE = "invalidCardState";
    private static String ALARM_LOCAL_OP_FAILED = "localOpFailed";
    private static String INVALID_CB_DATA = "invalidCbData";
    private static String FILE_UPLOAD_FAILED = "fileUploadFailed";
    private static String ALARM_INVALID_MERCHANT_STATE = "invalidMerchantState";

    // Exception logging methods
    public static void handleException(BackendlessException e) {
        Crashlytics.logException(e);
    }

    public static void handleException(Exception e) {
        Crashlytics.logException(e);
    }

    // Raise alarm method for each Alarm name/code
    // Alarm CSV string format:-
    // <time>,<userId>,<userType>,<severity>,<alarm class>,<alarm name>,<method name>,<params>
    // <params> are 'key=value' pairs, separated by ":"
    public static void deviceRooted(String userId, int userType, String methodName, Map<String,String> params) {

        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_WARN+ALARM_CSV_DELIM +
                ALARM_CLASS_SECURITY+ALARM_CSV_DELIM +
                ALARM_DEVICE_ROOTED+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }
    public static void noDataAvailable(String userId, int userType, String methodName, Map<String,String> params) {
        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_WARN+ALARM_CSV_DELIM +
                ALARM_CLASS_DATA_INTEGRITY+ALARM_CSV_DELIM +
                ALARM_NO_DATA+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }
    public static void invalidCardState(String userId, int userType, String methodName, Map<String,String> params) {
        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_FATAL+ALARM_CSV_DELIM +
                ALARM_CLASS_DATA_INTEGRITY+ALARM_CSV_DELIM +
                ALARM_INVALID_CARD_STATE+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }
    public static void localOpFailed(String userId, int userType, String methodName, Map<String,String> params) {
        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_ERROR+ALARM_CSV_DELIM +
                ALARM_IGNORE_IF_NOT_MANY+ALARM_CSV_DELIM +
                ALARM_LOCAL_OP_FAILED+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }
    public static void invalidCbData(String userId, int userType, String methodName, Map<String,String> params) {
        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_FATAL+ALARM_CSV_DELIM +
                ALARM_INVALID_STATE+ALARM_CSV_DELIM +
                INVALID_CB_DATA+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }
    public static void fileUploadFailed(String userId, int userType, String methodName, Map<String,String> params) {
        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_WARN+ALARM_CSV_DELIM +
                ALARM_UPLOAD_FAILED+ALARM_CSV_DELIM +
                FILE_UPLOAD_FAILED+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }
    public static void invalidMerchantState(String userId, int userType, String methodName, Map<String,String> params) {
        raiseAlarm(String.valueOf(System.currentTimeMillis())+ALARM_CSV_DELIM +
                userId+ALARM_CSV_DELIM +
                DbConstants.userTypeDesc[userType]+ALARM_CSV_DELIM +
                ALARM_SEVERITY_FATAL+ALARM_CSV_DELIM +
                ALARM_CLASS_DATA_INTEGRITY+ALARM_CSV_DELIM +
                ALARM_INVALID_MERCHANT_STATE+ALARM_CSV_DELIM +
                methodName+ALARM_CSV_DELIM +
                getParamStr(params));
    }


    private static String getParamStr(Map<String,String> params) {
        StringBuilder paramsStr = new StringBuilder();
        if(params!=null) {
            boolean firstEntry = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if(firstEntry) {
                    firstEntry = false;
                } else {
                    paramsStr.append(ALARM_PARAM_DELIM);
                }
                paramsStr.append(entry.getKey()).append("=").append(entry.getValue());
            }
        } else {
            paramsStr.append("");
        }
        return paramsStr.toString();
    }

    private static void raiseAlarm(String alarmMsg) {
        Crashlytics.logException(new Exception(alarmMsg));
    }

}
