package in.myecash.common.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 21-12-2016.
 */
public class GlobalSettingConstants {

    private static final int TOTAL_SETTINGS_CNT = 40;
    /*
     * Keys of all GlobalSettings
     */
    public static final String SETTINGS_MERCHANT_PASSWD_RESET_MINS = "Mchnt_Password_Reset_Mins";
    public static final String SETTINGS_CUSTOMER_PASSWD_RESET_MINS = "Cust_Password_Reset_Mins";
    public static final String SETTINGS_MERCHANT_ACCOUNT_BLOCK_MINS = "Mchnt_Acc_Locked_Mins";
    public static final String SETTINGS_CUSTOMER_ACCOUNT_BLOCK_MINS = "Cust_Acc_Locked_Mins";
    public static final String SETTINGS_CB_REDEEM_CARD_REQ = "CB_Redeem_Card_Required";
    public static final String SETTINGS_ACC_DB_CARD_REQ = "AC_Debit_Card_Required";
    public static final String SETTINGS_CL_CREDIT_LIMIT_FOR_PIN = "AC_Credit_Limit_For_PIN";
    public static final String SETTINGS_CL_DEBIT_LIMIT_FOR_PIN = "AC_Debit_Limit_For_PIN";
    public static final String SETTINGS_CB_DEBIT_LIMIT_FOR_PIN = "CB_Debit_Limit_For_PIN";
    public static final String SETTINGS_STATS_NO_REFRESH_MINS = "Mchnt_Stats_Refresh_Mins";
    public static final String SETTINGS_CUSTOMER_NO_REFRESH_MINS = "Cust_Data_Refresh_Mins";
    public static final String SETTINGS_CUSTOMER_CASH_LIMIT = "AC_Max_Cash_Limit";
    public static final String SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS = "Mchnt_Removal_Expiry_Days";
    public static final String SETTINGS_CUST_ACC_LIMIT_MODE_MINS = "Cust_Limited_Mode_Mins";
    public static final String SETTINGS_WRONG_ATTEMPT_RESET_MINS = "Wrong_Verify_Reset_Mins";
    // Txns older than this will be archived to files
    // This also means that txns older than this :
    // 1) Cannot be cancelled by Merchant
    // 2) Cannot be visible on main screen i.e. can be fetched on per Merchant basis only.
    public static final String SETTINGS_TXNS_INTABLE_KEEP_DAYS = "Txns_Recent_Days";

    public static final String SETTINGS_OPS_KEEP_DAYS = "Service_Req_Keep_Days";
    public static final String SETTINGS_OTP_VALID_MINS = "OTP_Valid_Mins";
    public static final String SETTINGS_MERCHANT_WRONG_ATTEMPT_LIMIT = "Mchnt_Allowed_Wrong_Verify_Attempts";
    public static final String SETTINGS_CUSTOMER_WRONG_ATTEMPT_LIMIT = "Cust_Allowed_Wrong_Verify_Attempts";
    public static final String SETTINGS_MCHNT_RENEW_DURATION = "Mchnt_Renew_Months";
    public static final String SETTINGS_CUST_RENEW_DURATION = "Cust_Renew_Months";
    public static final String SETTINGS_MCHNT_TXN_HISTORY_DAYS = "Mchnt_Txn_History_Days";
    public static final String SETTINGS_CUST_TXN_HISTORY_DAYS = "Cust_Txn_History_Days";
    public static final String SETTINGS_CB_REDEEM_LIMIT = "CB_Redeem_Limit";
    public static final String SETTINGS_SERVICE_DISABLED_UNTIL = "Service_Disabled_Until";
    public static final String SETTINGS_TXN_IMAGE_CAPTURE_MODE = "Txn_Image_Capture_Mode";
    public static final String SETTINGS_DAILY_DOWNTIME_START_HOUR = "Daily_Downtime_Start_Hour";
    public static final String SETTINGS_DAILY_DOWNTIME_END_HOUR = "Daily_Downtime_End_Hour";

    /*
     * Ones defined only in backend as constant values - as not used by App
     * Thus they are more of Backend Constants - but still kept here
     */
    public static final int FAILED_SMS_RETRY_MINS = 30;

    // Below are not part of global settings, but keeping them here
    // as they depend on above 'passwd reset mins' values - keep the values 1/6th of them
    // TODO: Keep the 'backend passwd reset timer' duration (below defined) 1/6th of 'password reset mins' values
    // So, if Cool_off_mins is 60, then timer should run every 10 mins
    public static final int MERCHANT_PASSWORD_RESET_TIMER_INTERVAL = 5;
    public static final int CUSTOMER_PASSWORD_RESET_TIMER_INTERVAL = 30;

    /*
     * Map to Values
     */
    // 'txn_image_capture_mode' global setting values
    public static final int TXN_IMAGE_CAPTURE_ALWAYS = 0;
    // only when 'card is mandatory' based on txn type and amounts
    public static final int TXN_IMAGE_CAPTURE_CARD_REQUIRED = 1;
    public static final int TXN_IMAGE_CAPTURE_NEVER = 2;

    public static final Map<String, String> valuesGlobalSettings;
    static {
        Map<String, String> aMap = new HashMap<>(TOTAL_SETTINGS_CNT);
        aMap.put(SETTINGS_MERCHANT_PASSWD_RESET_MINS,"10");
        aMap.put(SETTINGS_CUSTOMER_PASSWD_RESET_MINS,"1");
        aMap.put(SETTINGS_MERCHANT_ACCOUNT_BLOCK_MINS,"10");
        aMap.put(SETTINGS_CUSTOMER_ACCOUNT_BLOCK_MINS,"10");
        aMap.put(SETTINGS_CUST_ACC_LIMIT_MODE_MINS,"10");
        aMap.put(SETTINGS_STATS_NO_REFRESH_MINS,"10");
        aMap.put(SETTINGS_CUSTOMER_NO_REFRESH_MINS,"10");
        aMap.put(SETTINGS_WRONG_ATTEMPT_RESET_MINS,"120");

        aMap.put(SETTINGS_CB_REDEEM_CARD_REQ,"true");
        aMap.put(SETTINGS_ACC_DB_CARD_REQ,"true");
        aMap.put(SETTINGS_CL_CREDIT_LIMIT_FOR_PIN,"500");
        aMap.put(SETTINGS_CL_DEBIT_LIMIT_FOR_PIN,"0");
        aMap.put(SETTINGS_CB_DEBIT_LIMIT_FOR_PIN,"0");
        aMap.put(SETTINGS_CB_REDEEM_LIMIT,"200");
        aMap.put(SETTINGS_CUSTOMER_CASH_LIMIT,"500");

        aMap.put(SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS,"30");
        aMap.put(SETTINGS_TXNS_INTABLE_KEEP_DAYS,"2");
        aMap.put(SETTINGS_OPS_KEEP_DAYS,"90");
        aMap.put(SETTINGS_OTP_VALID_MINS,"10");
        aMap.put(SETTINGS_MERCHANT_WRONG_ATTEMPT_LIMIT,"5");
        aMap.put(SETTINGS_CUSTOMER_WRONG_ATTEMPT_LIMIT,"5");
        aMap.put(SETTINGS_MCHNT_RENEW_DURATION,"12");
        aMap.put(SETTINGS_CUST_RENEW_DURATION,"12");
        aMap.put(SETTINGS_MCHNT_TXN_HISTORY_DAYS,"90");
        aMap.put(SETTINGS_CUST_TXN_HISTORY_DAYS,"90");
        aMap.put(SETTINGS_SERVICE_DISABLED_UNTIL,null);
        aMap.put(SETTINGS_TXN_IMAGE_CAPTURE_MODE,String.valueOf(TXN_IMAGE_CAPTURE_CARD_REQUIRED));
        aMap.put(SETTINGS_DAILY_DOWNTIME_START_HOUR,"1");
        aMap.put(SETTINGS_DAILY_DOWNTIME_END_HOUR,"6");
        valuesGlobalSettings = Collections.unmodifiableMap(aMap);
    }

    /*
     * Map to Type of Description
     */
    public static final Map<String, String> descGlobalSettings;
    static {
        Map<String, String> aMap = new HashMap<>(TOTAL_SETTINGS_CNT);
        aMap.put(SETTINGS_MERCHANT_PASSWD_RESET_MINS, "Merchant: Minutes after which new password is sent on reset.");
        aMap.put(SETTINGS_CUSTOMER_PASSWD_RESET_MINS, "Customer: Minutes after which new password is sent on reset.");
        aMap.put(SETTINGS_MERCHANT_ACCOUNT_BLOCK_MINS, "Merchant: Hours for which account is kept Locked. Enabled automatically after this.");
        aMap.put(SETTINGS_CUSTOMER_ACCOUNT_BLOCK_MINS, "Merchant: Hours for which account is kept Locked. Enabled automatically after this.");
        aMap.put(SETTINGS_CB_REDEEM_CARD_REQ, "If Customer Card Mandatory to be scanned, for Cashback redeem ?");
        aMap.put(SETTINGS_ACC_DB_CARD_REQ, "If Customer Card Mandatory to be scanned, for Account debit ?");
        aMap.put(SETTINGS_CL_CREDIT_LIMIT_FOR_PIN, "Account Credit: Amount over which Customer PIN is asked during txn.");
        aMap.put(SETTINGS_CL_DEBIT_LIMIT_FOR_PIN, "Account Debit: Amount over which Customer PIN is asked during txn.");
        aMap.put(SETTINGS_CB_DEBIT_LIMIT_FOR_PIN, "Cashback Redeem: Amount over which Customer PIN is asked during txn.");
        aMap.put(SETTINGS_STATS_NO_REFRESH_MINS, null);
        aMap.put(SETTINGS_CUSTOMER_NO_REFRESH_MINS, null);
        aMap.put(SETTINGS_CUSTOMER_CASH_LIMIT, "Customer: Maximum amount that can be kept in any account of single merchant.");
        aMap.put(SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS, null);
        aMap.put(SETTINGS_CUST_ACC_LIMIT_MODE_MINS, "Customer: Hours for which account is kept in Limited Mode. Enabled automatically after this.");
        aMap.put(SETTINGS_WRONG_ATTEMPT_RESET_MINS, null);
        aMap.put(SETTINGS_TXNS_INTABLE_KEEP_DAYS, "Transactions can be cancelled within this many days.");
        aMap.put(SETTINGS_OPS_KEEP_DAYS, "Service Requests for any account older than these many days are Purged.");
        aMap.put(SETTINGS_OTP_VALID_MINS, "Time for which any sent OTP is valid.");
        aMap.put(SETTINGS_MERCHANT_WRONG_ATTEMPT_LIMIT, "Merchant: Number of wrong repeated verifications allowed. Account gets Locked after this.");
        aMap.put(SETTINGS_CUSTOMER_WRONG_ATTEMPT_LIMIT, "Customer: Number of wrong repeated verifications allowed. Account gets Locked after this.");
        aMap.put(SETTINGS_MCHNT_RENEW_DURATION, null);
        aMap.put(SETTINGS_CUST_RENEW_DURATION, null);
        aMap.put(SETTINGS_MCHNT_TXN_HISTORY_DAYS, "Merchant: Txns older than this cannot be viewed in App.");
        aMap.put(SETTINGS_CUST_TXN_HISTORY_DAYS, "Customer: Txns older than this cannot be viewed in App, for any particular merchant.");
        aMap.put(SETTINGS_CB_REDEEM_LIMIT, "Customer: Minimum Cashback amount for particular merchant, after which only it can be redeemed.");
        aMap.put(SETTINGS_SERVICE_DISABLED_UNTIL, "Exact time till which Service is Disabled.");
        aMap.put(SETTINGS_TXN_IMAGE_CAPTURE_MODE, null);
        aMap.put(SETTINGS_DAILY_DOWNTIME_START_HOUR, "Start Hour for Daily service downtime (24 hour format).");
        aMap.put(SETTINGS_DAILY_DOWNTIME_END_HOUR, "End Hour for Daily service downtime (24 hour format).");
        descGlobalSettings = Collections.unmodifiableMap(aMap);
    }

    /*
     * Map to Type of Values
     */
    // 'value_datatype' column values
    public static final int DATATYPE_INT = 1;
    public static final int DATATYPE_BOOLEAN = 2;
    public static final int DATATYPE_STRING = 3;
    public static final int DATATYPE_DATE = 4;

    public static final Map<String, Integer> valueTypesGlobalSettings;
    static {
        Map<String, Integer> aMap = new HashMap<>(TOTAL_SETTINGS_CNT);
        aMap.put(SETTINGS_MERCHANT_PASSWD_RESET_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_CUSTOMER_PASSWD_RESET_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_MERCHANT_ACCOUNT_BLOCK_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_CUSTOMER_ACCOUNT_BLOCK_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_CB_REDEEM_CARD_REQ, DATATYPE_BOOLEAN);
        aMap.put(SETTINGS_ACC_DB_CARD_REQ, DATATYPE_BOOLEAN);
        aMap.put(SETTINGS_CL_CREDIT_LIMIT_FOR_PIN, DATATYPE_INT);
        aMap.put(SETTINGS_CL_DEBIT_LIMIT_FOR_PIN, DATATYPE_INT);
        aMap.put(SETTINGS_CB_DEBIT_LIMIT_FOR_PIN, DATATYPE_INT);
        aMap.put(SETTINGS_STATS_NO_REFRESH_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_CUSTOMER_CASH_LIMIT, DATATYPE_INT);
        aMap.put(SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS, DATATYPE_INT);
        aMap.put(SETTINGS_CUST_ACC_LIMIT_MODE_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_WRONG_ATTEMPT_RESET_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_TXNS_INTABLE_KEEP_DAYS, DATATYPE_INT);
        aMap.put(SETTINGS_OPS_KEEP_DAYS, DATATYPE_INT);
        aMap.put(SETTINGS_OTP_VALID_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_MERCHANT_WRONG_ATTEMPT_LIMIT, DATATYPE_INT);
        aMap.put(SETTINGS_CUSTOMER_WRONG_ATTEMPT_LIMIT, DATATYPE_INT);
        aMap.put(SETTINGS_MCHNT_RENEW_DURATION, DATATYPE_INT);
        aMap.put(SETTINGS_CUST_RENEW_DURATION, DATATYPE_INT);
        aMap.put(SETTINGS_MCHNT_TXN_HISTORY_DAYS, DATATYPE_INT);
        aMap.put(SETTINGS_CUST_TXN_HISTORY_DAYS, DATATYPE_INT);
        aMap.put(SETTINGS_CUSTOMER_NO_REFRESH_MINS, DATATYPE_INT);
        aMap.put(SETTINGS_CB_REDEEM_LIMIT, DATATYPE_INT);
        aMap.put(SETTINGS_SERVICE_DISABLED_UNTIL, DATATYPE_DATE);
        aMap.put(SETTINGS_TXN_IMAGE_CAPTURE_MODE, DATATYPE_INT);
        aMap.put(SETTINGS_DAILY_DOWNTIME_START_HOUR, DATATYPE_INT);
        aMap.put(SETTINGS_DAILY_DOWNTIME_END_HOUR, DATATYPE_INT);
        valueTypesGlobalSettings = Collections.unmodifiableMap(aMap);
    }
}
