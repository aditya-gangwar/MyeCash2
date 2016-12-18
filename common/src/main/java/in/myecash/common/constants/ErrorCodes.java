package in.myecash.common.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 19-02-2016.
 */
public class ErrorCodes {

    // Non error responses
    public static final int NO_ERROR = 100;
    public static final int OP_SCHEDULED = 102;
    public static final int OTP_GENERATED = 103;

    // Generic error code
    public static final int GENERAL_ERROR = 200;

    // User and account related error codes
    public static final int USER_ALREADY_LOGGED_IN = 500;
    public static final int NO_SUCH_USER = 501;
    public static final int USER_ACC_DISABLED = 502;
    public static final int USER_ACC_LOCKED = 503;
    public static final int FIRST_LOGIN_PENDING = 504;
    public static final int NOT_LOGGED_IN = 505;
    public static final int CUST_NOT_REG_WITH_MCNT = 506;
    public static final int ACC_UNDER_EXPIRY = 507;
    public static final int USER_WRONG_ID_PASSWD = 508;
    public static final int FATAL_ERROR_ACC_DISABLED = 509;
    public static final int USER_ALREADY_REGISTERED = 510;
    public static final int LIMITED_ACCESS_CREDIT_TXN_ONLY = 511;

    // Input / Verification data or permissions failure
    public static final int WRONG_INPUT_DATA = 520;
    public static final int WRONG_OTP = 521;
    public static final int WRONG_PIN = 522;
    public static final int VERIFICATION_FAILED = 523;
    public static final int OPERATION_NOT_ALLOWED = 524;
    public static final int NOT_TRUSTED_DEVICE = 525;
    public static final int TEMP_PASSWD_EXPIRED = 526;
    public static final int WRONG_USER_TYPE = 527;
    public static final int DEVICE_INSECURE = 528;

    // Membership card errors
    public static final int NO_SUCH_CARD = 530;
    public static final int WRONG_CARD = 531;
    public static final int CARD_ALREADY_IN_USE = 532;
    public static final int CARD_DISABLED = 533;
    public static final int CARD_NOT_REG_WITH_CUST = 534;
    public static final int CARD_WRONG_OWNER_MCHNT = 535;

    // Limit based errors
    public static final int FAILED_ATTEMPT_LIMIT_RCHD = 540;
    public static final int TRUSTED_DEVICE_LIMIT_RCHD = 541;
    public static final int CASH_ACCOUNT_LIMIT_RCHD = 542;

    // Format related errors
    public static final int EMPTY_VALUE = 550;
    public static final int INVALID_FORMAT = 551;
    public static final int INVALID_LENGTH = 552;
    public static final int INVALID_VALUE = 553;
    public static final int NO_DATA_FOUND = 554;

    // Any of sub backend operations failed
    public static final int SEND_SMS_FAILED = 560;
    public static final int OTP_GENERATE_FAILED = 562;

    // Misc errors
    public static final int DUPLICATE_ENTRY = 661;
    public static final int DEVICE_ALREADY_REGISTERED = 662;
    public static final int MERCHANT_ID_RANGE_ERROR = 663;
    public static final int NO_INTERNET_CONNECTION = 664;
    public static final int FILE_UPLOAD_FAILED = 665;
    public static final int FILE_NOT_FOUND = 666;
    public static final int SERVICE_GLOBAL_DISABLED = 667;
    public static final int REMOTE_SERVICE_NOT_AVAILABLE = 668;
    public static final int MOBILE_ALREADY_REGISTERED = 669;

    // *******************************************************************
    // IT IS MANDATORY THAT ALL ERROR CODES ABOVE ARE ADDED TO BELOW MAP
    // EVEN IF WITH EMPTY STRING
    // *******************************************************************
    public static final Map<Integer, String> appErrorDesc;
    static {
        Map<Integer, String> aMap = new HashMap<>(100);

        aMap.put(NO_ERROR, "");
        aMap.put(OP_SCHEDULED,"");
        aMap.put(OTP_GENERATED,"OTP sent on mobile number. Do the operation again with OTP value.");

        aMap.put(GENERAL_ERROR, "System Error. Please try again.");

        aMap.put(USER_ALREADY_LOGGED_IN, "User is not logged in. Please login first.");
        aMap.put(NO_SUCH_USER,"User is not registered. Please register first and then try again.");
        aMap.put(USER_ACC_DISABLED, "User account is disabled. Please contact customer care.");
        aMap.put(USER_ACC_LOCKED,"User account is temporarily locked.");
        aMap.put(FIRST_LOGIN_PENDING, "New user. Please use forgot password link to generate new password.");
        aMap.put(NOT_LOGGED_IN,"User not logged in.");
        aMap.put(CUST_NOT_REG_WITH_MCNT,"Customer has done no transaction with the merchant.");
        aMap.put(ACC_UNDER_EXPIRY,"Account under Expiry duration");
        aMap.put(USER_WRONG_ID_PASSWD, "Wrong User id or Password. Account gets Locked after %s incorrect attempts.");
        aMap.put(FATAL_ERROR_ACC_DISABLED,"User account disabled temporarily by system for safety purpose. You will receive notification from customer care in next 24-48 hours.");
        aMap.put(USER_ALREADY_REGISTERED, "User is already registered");
        aMap.put(LIMITED_ACCESS_CREDIT_TXN_ONLY,"Limited Access. Only 'CREDIT' transactions are allowed.");

        aMap.put(SEND_SMS_FAILED,"Sorry, but we failed to send SMS to you. Request to please try again later.");
        aMap.put(OTP_GENERATE_FAILED,"Failed to generate OTP. Please try again later.");

        aMap.put(WRONG_INPUT_DATA,"Invalid input data");
        aMap.put(WRONG_OTP,"Wrong OTP value.");
        aMap.put(WRONG_PIN,"Wrong PIN. Account gets Locked after %s incorrect attempts.");
        aMap.put(VERIFICATION_FAILED,"Verification failed. Account gets Locked after %s incorrect attempts.");
        aMap.put(OPERATION_NOT_ALLOWED,"You do not have permissions for this operation");
        aMap.put(NOT_TRUSTED_DEVICE,"This device is not in trusted device list");
        aMap.put(TEMP_PASSWD_EXPIRED,"Temporary password expired. Please generate new password using 'Forget Password' link on login screen.");
        aMap.put(WRONG_USER_TYPE,"Wrong user type");
        aMap.put(DEVICE_INSECURE,"Your device is not secure. Please install and run application on other device.");

        aMap.put(NO_SUCH_CARD,"Invalid Membership Card");
        aMap.put(WRONG_CARD,"Invalid Membership card. Please return to company agent for refund.");
        aMap.put(CARD_ALREADY_IN_USE,"This Membership card is already registered to customer");
        aMap.put(CARD_DISABLED,"This Membership card is Disabled and cannot be used");
        aMap.put(CARD_NOT_REG_WITH_CUST,"This Membership card is not registered to any Customer");
        aMap.put(CARD_WRONG_OWNER_MCHNT,"This Membership card is not allocated to you as Merchant. Please return to company agent.");

        aMap.put(FAILED_ATTEMPT_LIMIT_RCHD,"Failed attempt limit reached. Account is Locked for next %s hours.");
        aMap.put(TRUSTED_DEVICE_LIMIT_RCHD,"Trusted device limit reached. To continue, login from any trusted device and delete any from the trusted devices.");
        aMap.put(CASH_ACCOUNT_LIMIT_RCHD,"Cash Account balance more than INR %s. Change 'Cash Paid'.");


        aMap.put(EMPTY_VALUE,"Empty input value");
        aMap.put(INVALID_FORMAT,"Invalid format");
        aMap.put(INVALID_LENGTH,"Invalid length");
        aMap.put(INVALID_VALUE,"Wrong input values");
        aMap.put(NO_DATA_FOUND,"No data found");

        aMap.put(DUPLICATE_ENTRY,"Duplicate entry. Data already exists.");
        aMap.put(DEVICE_ALREADY_REGISTERED,"Device already registered for other merchant. One device can register to only one merchant account.");
        aMap.put(MERCHANT_ID_RANGE_ERROR,"Issue with Merchant ID Range.");
        aMap.put(NO_INTERNET_CONNECTION,"Please check Internet connectivity and try again.");
        aMap.put(FILE_UPLOAD_FAILED,"Failed to upload the file. Please try again later.");
        aMap.put(FILE_NOT_FOUND,"Requested data not available");
        aMap.put(SERVICE_GLOBAL_DISABLED,"Service under maintenance. Please try after ");
        aMap.put(REMOTE_SERVICE_NOT_AVAILABLE,"MyeCash Server not reachable. Please check Internet connection.");
        aMap.put(MOBILE_ALREADY_REGISTERED,"Mobile Number is already registered for other user.");

        appErrorDesc = Collections.unmodifiableMap(aMap);
    }

    // These are defined by backendless
    // These are mapped to appropriate local error codes
    public static final String BL_ERROR_ENTITY_WITH_ID_NOT_FOUND = "1000";
    public static final String BL_ERROR_NO_PERMISSIONS = "1011";
    public static final String BL_ERROR_NO_PERMISSIONS_1 = "1012";
    public static final String BL_ERROR_NO_PERMISSIONS_2 = "1013";
    public static final String BL_ERROR_NO_PERMISSIONS_3 = "1014";
    public static final String BL_ERROR_NO_PERMISSIONS_4 = "1134";
    public static final String BL_ERROR_NO_PERMISSIONS_5 = "2000";
    public static final String BL_ERROR_NO_PERMISSIONS_6 = "2003";
    public static final String BL_ERROR_NO_DATA_FOUND = "1009";
    public static final String BL_ERROR_NO_DATA_FOUND_1 = "1033";
    public static final String BL_ERROR_NO_DATA_FOUND_2 = "1034";
    public static final String BL_ERROR_NO_DATA_FOUND_3 = "1035";
    public static final String BL_ERROR_DUPLICATE_ENTRY = "1155";
    public static final String BL_ERROR_DUPLICATE_ENTRY_1 = "1101";
    public static final String BL_ERROR_DUPLICATE_ENTRY_2 = "8001";
    public static final String BL_ERROR_REGISTER_DUPLICATE = "3033";
    public static final String BL_ERROR_LOGIN_DISABLED = "3000";
    public static final String BL_ERROR_ALREADY_LOGGOED_IN = "3002";
    public static final String BL_ERROR_INVALID_ID_PASSWD = "3003";
    public static final String BL_ERROR_EMPTY_ID_PASSWD = "3006";
    public static final String BL_ERROR_ACCOUNT_LOCKED = "3036";
    public static final String BL_ERROR_MULTIPLE_LOGIN_LIMIT = "3044";

    // Map from backendless error codes to local error codes
    public static final Map<String, Integer> backendToLocalErrorCode;
    static {
        Map<String, Integer> map = new HashMap<>(50);

        // backendless expected error codes
        map.put(BL_ERROR_LOGIN_DISABLED, USER_ACC_DISABLED);
        map.put(BL_ERROR_ALREADY_LOGGOED_IN, USER_ALREADY_LOGGED_IN);
        map.put(BL_ERROR_MULTIPLE_LOGIN_LIMIT, USER_ALREADY_LOGGED_IN);
        map.put(BL_ERROR_INVALID_ID_PASSWD, USER_WRONG_ID_PASSWD);
        map.put(BL_ERROR_EMPTY_ID_PASSWD, USER_WRONG_ID_PASSWD);
        map.put(BL_ERROR_ACCOUNT_LOCKED, USER_ACC_LOCKED);
        map.put(BL_ERROR_DUPLICATE_ENTRY, DUPLICATE_ENTRY);
        map.put(BL_ERROR_DUPLICATE_ENTRY_1, DUPLICATE_ENTRY);
        map.put(BL_ERROR_DUPLICATE_ENTRY_2, DUPLICATE_ENTRY);
        map.put(BL_ERROR_REGISTER_DUPLICATE, USER_ALREADY_REGISTERED);
        map.put(BL_ERROR_NO_PERMISSIONS, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_PERMISSIONS_1, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_PERMISSIONS_2, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_PERMISSIONS_3, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_PERMISSIONS_4, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_PERMISSIONS_5, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_PERMISSIONS_6, OPERATION_NOT_ALLOWED);
        map.put(BL_ERROR_NO_DATA_FOUND, NO_DATA_FOUND);
        map.put(BL_ERROR_NO_DATA_FOUND_1, NO_DATA_FOUND);
        map.put(BL_ERROR_NO_DATA_FOUND_2, NO_DATA_FOUND);
        map.put(BL_ERROR_NO_DATA_FOUND_3, NO_DATA_FOUND);

        backendToLocalErrorCode = Collections.unmodifiableMap(map);
    }
}
