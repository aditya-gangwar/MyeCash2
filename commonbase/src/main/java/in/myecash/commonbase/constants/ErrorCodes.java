package in.myecash.commonbase.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 19-02-2016.
 */
public class ErrorCodes {

    public static final String ERROR_Y_M_I_Here = "I shouldn't be here !!";

    // App common codes
    public static final int NO_ERROR = -1;
    public static final int GENERAL_ERROR = 0;

    // Merchant user operation error codes
    public static final int USER_ALREADY_REGISTERED = 1;
    public static final int USER_ALREADY_LOGGED_IN = 2;
    public static final int USER_NEW = 3;
    public static final int USER_ACC_DISABLED = 4;
    public static final int USER_WRONG_ID_PASSWD = 5;
    public static final int USER_ACC_LOCKED = 6;
    public static final int FILE_UPLOAD_FAILED = 7;
    public static final int USER_NOT_REGISTERED = 8;
    public static final int NO_INTERNET_CONNECTION = 9;
    public static final int EMPTY_VALUE = 10;
    public static final int INVALID_FORMAT = 11;
    public static final int INVALID_LENGTH = 12;
    public static final int INVALID_VALUE = 13;
    public static final int NO_DATA_FOUND = 14;
    public static final int CARD_ALREADY_IN_USE = 15;
    public static final int TEMP_PASSWD_EXPIRED = 16;
    public static final int SERVICE_GLOBAL_DISABLED = 17;
    public static final int WRONG_SECRET_PIN = 18;
    public static final int CARD_WRONG_MERCHANT = 19;
    public static final int WRONG_USER_TYPE = 20;
    // disabled by system due to detection of any data integrity violation
    public static final int FATAL_ERROR_ACC_DISABLED = 21;
    // user account temporarily blocked
    //public static final int USER_ACC_DISABLED_WRONG_PIN = 22;
    public static final int OTP_GENERATED = 23;
    public static final int WRONG_OTP = 24;
    public static final int WRONG_CARD = 25;
    public static final int OTP_GENERATE_FAILED = 26;
    public static final int SEND_SMS_FAILED = 27;
    public static final int WRONG_INPUT_DATA = 28;
    public static final int NO_SUCH_CARD = 29;
    public static final int VERIFICATION_FAILED = 30;
    public static final int FAILED_ATTEMPT_LIMIT_RCHD = 31;
    public static final int NOT_TRUSTED_DEVICE = 32;
    public static final int TRUSTED_DEV_LIMIT_RCHD = 33;
    public static final int OPERATION_SCHEDULED = 34;
    public static final int CARD_BLOCKED = 35;
    public static final int DUPLICATE_ENTRY = 36;
    public static final int CASH_ACCOUNT_LIMIT_RCHD = 37;
    public static final int FILE_NOT_FOUND = 38;
    public static final int MERCHANT_ID_RANGE_ERROR = 40;
    public static final int NO_PERMISSIONS = 41;
    public static final int CUST_REG_OK_CB_CREATE_FAILED = 42;
    public static final int DEVICE_ALREADY_REGISTERED = 43;
    public static final int DEVICE_INSECURE = 44;

    public static final Map<Integer, String> appErrorDesc;
    static {
        Map<Integer, String> aMap = new HashMap<>(100);

        aMap.put(GENERAL_ERROR, "System Error. Please try again.");
        aMap.put(USER_ALREADY_REGISTERED, "User is already registered.");
        aMap.put(USER_ALREADY_LOGGED_IN, "User is not logged in. Please login first.");
        aMap.put(USER_NEW, "New user. Please use forgot password link to generate new password.");
        aMap.put(USER_ACC_DISABLED, "User account is disabled. Please contact customer care.");
        aMap.put(USER_WRONG_ID_PASSWD, "Wrong user id or password. Please try again.");
        aMap.put(USER_ACC_LOCKED,"User account is temporarily locked.");
        aMap.put(FILE_UPLOAD_FAILED,"Failed to upload the file. Please try again later.");
        aMap.put(USER_NOT_REGISTERED,"User is not registered. Please register first and then try again.");
        aMap.put(NO_INTERNET_CONNECTION,"Please check internet connectivity and try again.");
        aMap.put(EMPTY_VALUE,"Empty input value");
        aMap.put(INVALID_FORMAT,"Invalid format");
        aMap.put(INVALID_LENGTH,"Invalid length");
        aMap.put(INVALID_VALUE,"Wrong input values");
        aMap.put(NO_DATA_FOUND,"No data found");
        aMap.put(CARD_ALREADY_IN_USE,"Membership card already in use");
        aMap.put(TEMP_PASSWD_EXPIRED,"Temporary password expired. Please generate new password using 'Forget Password' link on login screen.");
        aMap.put(SERVICE_GLOBAL_DISABLED,"Service under maintenance. Please try after ");
        aMap.put(WRONG_SECRET_PIN,"Wrong PIN");
        aMap.put(CARD_WRONG_MERCHANT,"Membership card not allotted to current merchant.");
        aMap.put(WRONG_USER_TYPE,"Wrong user type");
        aMap.put(FATAL_ERROR_ACC_DISABLED,"User account disabled temporarily by system for safety purpose. You will receive notification from customer care in next 24-48 hours.");
        aMap.put(OTP_GENERATED,"OTP sent on mobile number. Do the operation again with OTP value.");
        aMap.put(WRONG_OTP,"Wrong OTP value");
        aMap.put(WRONG_CARD,"Invalid customer card.");
        aMap.put(OTP_GENERATE_FAILED,"Failed to generate OTP. Please tru again later.");
        aMap.put(SEND_SMS_FAILED,"Failed to send SMS");
        aMap.put(WRONG_INPUT_DATA,"");
        aMap.put(NO_SUCH_CARD,"Invalid customer card");
        aMap.put(VERIFICATION_FAILED,"Request verification failed");
        aMap.put(FAILED_ATTEMPT_LIMIT_RCHD,"Failed attempt limit reached. This account is locked temporarily for next %s hours.");
        aMap.put(NOT_TRUSTED_DEVICE,"This device is not in trusted device list");
        aMap.put(TRUSTED_DEV_LIMIT_RCHD,"Trusted device limit reached. To continue, login from any trusted device and delete any from the trusted devices.");
        aMap.put(OPERATION_SCHEDULED,"");
        aMap.put(CARD_BLOCKED,"Customer card is blocked.");
        aMap.put(DUPLICATE_ENTRY,"Duplicate entry. Data already exists.");
        aMap.put(CASH_ACCOUNT_LIMIT_RCHD,"Cash account balance cannot exceed INR %s.");
        aMap.put(FILE_NOT_FOUND,"");
        aMap.put(MERCHANT_ID_RANGE_ERROR,"Issue with Merchant ID Range.");
        aMap.put(NO_PERMISSIONS,"You do not have permissions for this operation");
        aMap.put(CUST_REG_OK_CB_CREATE_FAILED,"");
        aMap.put(DEVICE_ALREADY_REGISTERED,"Device already registered for other merchant. One device can register to only one merchant account.");
        aMap.put(DEVICE_INSECURE,"Your device is not secure. Please install and run application on other device.");

        appErrorDesc = Collections.unmodifiableMap(aMap);
    };

    // Map from backend error codes to local error codes
    public static final Map<String, Integer> backendToLocalErrorCode;
    static {
        Map<String, Integer> aMap = new HashMap<>(100);

        // my own backend response codes
        aMap.put(BackendResponseCodes.BE_RESPONSE_NO_ERROR,NO_ERROR);
        aMap.put(BackendResponseCodes.BE_RESPONSE_OP_SCHEDULED,OPERATION_SCHEDULED);

        aMap.put(BackendResponseCodes.BE_ERROR_GENERAL,GENERAL_ERROR);
        aMap.put(BackendResponseCodes.BE_ERROR_NO_SUCH_USER,USER_NOT_REGISTERED);
        aMap.put(BackendResponseCodes.BE_ERROR_ACC_DISABLED,USER_ACC_DISABLED);
        aMap.put(BackendResponseCodes.BE_ERROR_ACC_LOCKED,USER_ACC_LOCKED);
        aMap.put(BackendResponseCodes.BE_ERROR_OPERATION_NOT_ALLOWED,NO_PERMISSIONS);
        aMap.put(BackendResponseCodes.BE_ERROR_DUPLICATE_REQUEST,DUPLICATE_ENTRY);
        aMap.put(BackendResponseCodes.BE_ERROR_FIRST_LOGIN_PENDING,USER_NEW);

        aMap.put(BackendResponseCodes.BE_ERROR_SEND_SMS_FAILED,SEND_SMS_FAILED);
        aMap.put(BackendResponseCodes.BE_ERROR_WRONG_INPUT_DATA,WRONG_INPUT_DATA);
        aMap.put(BackendResponseCodes.BE_ERROR_DUPLICATE_USER,USER_ALREADY_REGISTERED);

        aMap.put(BackendResponseCodes.BE_ERROR_OTP_GENERATE_FAILED,OTP_GENERATE_FAILED);
        aMap.put(BackendResponseCodes.BE_RESPONSE_OTP_GENERATED,OTP_GENERATED);
        aMap.put(BackendResponseCodes.BE_ERROR_WRONG_OTP,WRONG_OTP);
        aMap.put(BackendResponseCodes.BE_ERROR_WRONG_PIN,WRONG_SECRET_PIN);

        aMap.put(BackendResponseCodes.BE_ERROR_NO_SUCH_CARD,NO_SUCH_CARD);
        aMap.put(BackendResponseCodes.BE_ERROR_WRONG_CARD,WRONG_CARD);
        aMap.put(BackendResponseCodes.BE_ERROR_CARD_INUSE,CARD_ALREADY_IN_USE);
        aMap.put(BackendResponseCodes.BE_ERROR_CARD_WRONG_MERCHANT,CARD_WRONG_MERCHANT);
        aMap.put(BackendResponseCodes.BE_ERROR_CARD_BLOCKED,CARD_BLOCKED);

        aMap.put(BackendResponseCodes.BE_ERROR_VERIFICATION_FAILED,VERIFICATION_FAILED);
        aMap.put(BackendResponseCodes.BE_ERROR_FAILED_ATTEMPT_LIMIT_RCHD,FAILED_ATTEMPT_LIMIT_RCHD);
        aMap.put(BackendResponseCodes.BE_ERROR_NOT_TRUSTED_DEVICE,NOT_TRUSTED_DEVICE);
        aMap.put(BackendResponseCodes.BE_ERROR_TRUSTED_DEVICE_LIMIT_RCHD,TRUSTED_DEV_LIMIT_RCHD);
        aMap.put(BackendResponseCodes.BE_ERROR_CASH_ACCOUNT_LIMIT_RCHD,CASH_ACCOUNT_LIMIT_RCHD);
        aMap.put(BackendResponseCodes.BE_ERROR_DEVICE_ALREADY_REGISTERED,DEVICE_ALREADY_REGISTERED);

        aMap.put(BackendResponseCodes.BE_ERROR_NO_OPEN_MERCHANT_ID_BATCH,MERCHANT_ID_RANGE_ERROR);

        // backendless expected error codes
        aMap.put(BackendResponseCodes.BL_ERROR_LOGIN_DISABLED,USER_ACC_DISABLED);
        aMap.put(BackendResponseCodes.BL_ERROR_ALREADY_LOGGOED_IN,USER_ALREADY_LOGGED_IN);
        aMap.put(BackendResponseCodes.BL_ERROR_MULTIPLE_LOGIN_LIMIT,USER_ALREADY_LOGGED_IN);
        aMap.put(BackendResponseCodes.BL_ERROR_INVALID_ID_PASSWD,USER_WRONG_ID_PASSWD);
        aMap.put(BackendResponseCodes.BL_ERROR_EMPTY_ID_PASSWD,USER_WRONG_ID_PASSWD);
        aMap.put(BackendResponseCodes.BL_ERROR_ACCOUNT_LOCKED,USER_ACC_LOCKED);
        aMap.put(BackendResponseCodes.BL_ERROR_DUPLICATE_ENTRY,DUPLICATE_ENTRY);
        aMap.put(BackendResponseCodes.BL_ERROR_REGISTER_DUPLICATE,USER_ALREADY_REGISTERED);
        aMap.put(BackendResponseCodes.BL_ERROR_NO_PERMISSIONS,NO_PERMISSIONS);

        backendToLocalErrorCode = Collections.unmodifiableMap(aMap);
    }
}
