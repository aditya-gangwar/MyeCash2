package in.myecash.common;

/**
 * Created by adgangwa on 19-02-2016.
 */

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.database.GlobalSettings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MyGlobalSettings
{
    /*
     * Defines the mode in which code is running
     */
    public enum RunMode {
        appMerchant,
        appCustomer,
        appInternalUser,
        backend
    }
    private static RunMode mRunMode;
    public static RunMode getRunMode(RunMode runMode) {
        return runMode;
    }

    public static Map<String,Object> mSettings;
    public static List<gSettings> userVisibleSettings = new ArrayList<>();

    public static class gSettings {
        public String name;
        public String value;
        public String description;
        public Date updated;
    }

    /*
     * Names of settings - should match values of 'name' column in GlobalSettings table
     *
     * Also defined are constant values against the some of the
     * columns of GlobalSettings table, which are used in backend codes too.
     * This is to avoid fetching GlobalSettings table in backend code each time.
     * This means: These values should be manually kept in sync with those in GlobalSettings table.
     * User apps fetch values from 'GlobalSettings' table,
     * but backend code use below constants for the same.
     */

    /*
     * Ones defined in both DB and 'as constants' - so should be synced
     * TODO: cross verify these 'constant values' with that in the DB
     */
    private static final String SETTINGS_MERCHANT_PASSWD_RESET_MINS = "merchant_passwd_reset_mins";
    private static final int MERCHANT_PASSWORD_RESET_COOL_OFF_MINS = 15;

    private static final String SETTINGS_CUSTOMER_PASSWD_RESET_MINS = "customer_passwd_reset_mins";
    private static final int CUSTOMER_PASSWORD_RESET_COOL_OFF_MINS = 15;

    private static final String SETTINGS_MERCHANT_ACCOUNT_BLOCK_HRS = "merchant_account_block_hrs";
    private static final int MERCHANT_ACCOUNT_BLOCKED_HOURS = 1;

    private static final String SETTINGS_CUSTOMER_ACCOUNT_BLOCK_HRS = "customer_account_block_hrs";
    private static final int CUSTOMER_ACCOUNT_BLOCKED_HOURS = 24;

    private static final String SETTINGS_CB_REDEEM_CARD_REQ = "cb_redeem_card_req";
    private static final boolean CB_REDEEM_CARD_REQ = true;

    private static final String SETTINGS_ACC_DB_CARD_REQ = "acc_debit_card_req";
    private static final boolean ACC_DEBIT_CARD_REQ = true;

    private static final String SETTINGS_CL_CREDIT_LIMIT_FOR_PIN = "cl_credit_limit_for_pin";
    private static final int CL_CREDIT_LIMIT_FOR_PIN = 10;

    private static final String SETTINGS_CL_DEBIT_LIMIT_FOR_PIN = "cl_debit_limit_for_pin";
    private static final int CL_DEBIT_LIMIT_FOR_PIN = 0;

    private static final String SETTINGS_CB_DEBIT_LIMIT_FOR_PIN = "cb_debit_limit_for_pin";
    private static final int CB_DEBIT_LIMIT_FOR_PIN = 20;

    private static final String SETTINGS_STATS_NO_REFRESH_HRS = "mchnt_stats_no_refresh_hours";
    private static final int MCHNT_STATS_NO_REFRESH_HOURS = 1;

    private static final String SETTINGS_CUSTOMER_CASH_LIMIT = "cash_account_max_limit";
    private static final int CUSTOMER_CASH_MAX_LIMIT = 500;

    private static final String SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS = "mchnt_removal_expiry_days";
    private static final int MCHNT_REMOVAL_EXPIRY_DAYS = 30;

    private static final String SETTINGS_CUST_HRS_AFTER_MOB_CHANGE = "cust_hrs_after_mob_change";
    private static final int CUST_HRS_AFTER_MOB_CHANGE = 6;

    private static final String SETTINGS_WRONG_ATTEMPT_RESET_HRS = "wrong_attempt_reset_hrs";
    private static final int WRONG_ATTEMPT_RESET_HRS = 2;

    private static final String SETTINGS_TXNS_INTABLE_KEEP_DAYS = "txns_intable_keep_days";
    private static final int CUST_TXNS_KEEP_DAYS = 5;

    private static final String SETTINGS_OPS_KEEP_DAYS = "ops_keep_days";
    private static final int OPS_KEEP_DAYS = 90;


    /*
     * Ones defined only in DB - as used only by Apps and not backend
     */
    private static final String SETTINGS_MCHNT_RENEW_DURATION = "mchnt_renewal_duration";
    private static final String SETTINGS_CUST_RENEW_DURATION = "cust_renewal_duration";
    private static final String SETTINGS_MCHNT_TXN_HISTORY_DAYS = "mchnt_txn_history_days";
    private static final String SETTINGS_CUST_TXN_HISTORY_DAYS = "cust_txn_history_days";
    private static final String SETTINGS_CUSTOMER_NO_REFRESH_HRS = "cust_no_refresh_hrs";
    private static final String SETTINGS_CB_REDEEM_LIMIT = "cb_redeem_limit";
    private static final String SETTINGS_SERVICE_DISABLED_UNTIL = "service_disabled_until";
    private static final String SETTINGS_TXN_IMAGE_CAPTURE_MODE = "txn_image_capture_mode";


    /*
     * Ones defined only in backend as constant values - as not used by App
     */
    public static final int FAILED_SMS_RETRY_MINS = 30;
    public static final int OTP_VALID_MINS = 10;

    private static final int MERCHANT_WRONG_ATTEMPT_LIMIT = 5;
    private static final int CUSTOMER_WRONG_ATTEMPT_LIMIT = 5;
    private static final int INTERNAL_USER_WRONG_ATTEMPT_LIMIT = 5;
    // Map of user type to above values
    public static final Map<Integer, Integer> userTypeToWrongLimit;
    static {
        Map<Integer, Integer> aMap = new HashMap<>(10);
        aMap.put(DbConstants.USER_TYPE_MERCHANT,MERCHANT_WRONG_ATTEMPT_LIMIT);
        aMap.put(DbConstants.USER_TYPE_CUSTOMER,CUSTOMER_WRONG_ATTEMPT_LIMIT);
        aMap.put(DbConstants.USER_TYPE_AGENT,INTERNAL_USER_WRONG_ATTEMPT_LIMIT);
        aMap.put(DbConstants.USER_TYPE_CC,INTERNAL_USER_WRONG_ATTEMPT_LIMIT);
        aMap.put(DbConstants.USER_TYPE_CNT,INTERNAL_USER_WRONG_ATTEMPT_LIMIT);
        userTypeToWrongLimit = Collections.unmodifiableMap(aMap);
    }

    // Below are not part of global settings, but keeping them here
    // as they depend on above 'passwd reset mins' values - keep the values 1/6th of them
    // TODO: Keep the 'backend passwd reset timer' duration (below defined) 1/6th of 'password reset mins' values
    // So, if Cool_off_mins is 60, then timer should run every 10 mins
    public static final int MERCHANT_PASSWORD_RESET_TIMER_INTERVAL = 10;
    public static final int CUSTOMER_PASSWORD_RESET_TIMER_INTERVAL = 10;



    /*
     * Access Functions
     */
    public static Integer getMchntPasswdResetMins() {
        return (mRunMode==RunMode.backend)?
                MERCHANT_PASSWORD_RESET_COOL_OFF_MINS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_MERCHANT_PASSWD_RESET_MINS);
    }
    public static Integer getCustPasswdResetMins() {
        return (mRunMode==RunMode.backend)?
                CUSTOMER_PASSWORD_RESET_COOL_OFF_MINS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUSTOMER_PASSWD_RESET_MINS);
    }

    public static Integer getAccBlockHrs(Integer userType) {
        if(userType==DbConstants.USER_TYPE_CUSTOMER || mRunMode==RunMode.appCustomer) {
            return (mRunMode==RunMode.backend)?
                    CUSTOMER_ACCOUNT_BLOCKED_HOURS:
                    (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUSTOMER_ACCOUNT_BLOCK_HRS);
        }
        // For Merchant and all others
        return (mRunMode==RunMode.backend)?
                MERCHANT_ACCOUNT_BLOCKED_HOURS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_MERCHANT_ACCOUNT_BLOCK_HRS);
    }

    public static Boolean getCardReqCbRedeem() {
        return (mRunMode==RunMode.backend)?
                CB_REDEEM_CARD_REQ:
                (Boolean)MyGlobalSettings.mSettings.get(SETTINGS_CB_REDEEM_CARD_REQ);
    }
    public static Boolean getCardReqAccDebit() {
        return (mRunMode==RunMode.backend)?
                ACC_DEBIT_CARD_REQ:
                (Boolean)MyGlobalSettings.mSettings.get(SETTINGS_ACC_DB_CARD_REQ);
    }

    public static Integer getMchntRenewalDuration() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_MCHNT_RENEW_DURATION);
    }
    public static Integer getCustRenewalDuration() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUST_RENEW_DURATION);
    }

    public static Integer getAccAddPinLimit() {
        return (mRunMode==RunMode.backend)?
                CL_CREDIT_LIMIT_FOR_PIN:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CL_CREDIT_LIMIT_FOR_PIN);
    }
    public static Integer getAccDebitPinLimit() {
        return (mRunMode==RunMode.backend)?
                CL_DEBIT_LIMIT_FOR_PIN:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CL_DEBIT_LIMIT_FOR_PIN);
    }
    public static Integer getCbDebitPinLimit() {
        return (mRunMode==RunMode.backend)?
                CB_DEBIT_LIMIT_FOR_PIN:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CB_DEBIT_LIMIT_FOR_PIN);
    }

    public static Integer getMchntTxnHistoryDays() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_MCHNT_TXN_HISTORY_DAYS);
    }

    public static Integer getCustTxnHistoryDays() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUST_TXN_HISTORY_DAYS);
    }

    public static Integer getCustNoRefreshHrs() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUSTOMER_NO_REFRESH_HRS);
    }

    public static Integer getMchntDashBNoRefreshHrs() {
        return (mRunMode==RunMode.backend)?
                MCHNT_STATS_NO_REFRESH_HOURS:
            (Integer)MyGlobalSettings.mSettings.get(SETTINGS_STATS_NO_REFRESH_HRS);
    }

    public static Integer getCashAccLimit() {
        return (mRunMode==RunMode.backend)?
                CUSTOMER_CASH_MAX_LIMIT:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUSTOMER_CASH_LIMIT);
    }

    public static Integer getMchntExpiryDays() {
        return (mRunMode==RunMode.backend)?
                MCHNT_REMOVAL_EXPIRY_DAYS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS);
    }

    public static Integer getCustHrsAfterMobChange() {
        return (mRunMode==RunMode.backend)?
                CUST_HRS_AFTER_MOB_CHANGE:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CUST_HRS_AFTER_MOB_CHANGE);
    }

    public static Integer getCbRedeemLimit() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_CB_REDEEM_LIMIT);
    }

    public static Date getServiceDisabledUntil() {
        return (Date)MyGlobalSettings.mSettings.get(SETTINGS_SERVICE_DISABLED_UNTIL);
    }

    public static Integer getCardImageCaptureMode() {
        return (Integer)MyGlobalSettings.mSettings.get(SETTINGS_TXN_IMAGE_CAPTURE_MODE);
    }

    public static Integer getWrongAttemptResetHrs() {
        return (mRunMode==RunMode.backend)?
                WRONG_ATTEMPT_RESET_HRS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_WRONG_ATTEMPT_RESET_HRS);
    }

    public static Integer getTxnsIntableKeepDays() {
        return (mRunMode==RunMode.backend)?
                CUST_TXNS_KEEP_DAYS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_TXNS_INTABLE_KEEP_DAYS);
    }

    public static Integer getOpsKeepDays() {
        return (mRunMode==RunMode.backend)?
                OPS_KEEP_DAYS:
                (Integer)MyGlobalSettings.mSettings.get(SETTINGS_OPS_KEEP_DAYS);
    }

    /*
     * Valid Values for some of column in GlobalSettings table
     */
    // 'txn_image_capture_mode' column values
    public static final int TXN_IMAGE_CAPTURE_ALWAYS = 0;
    // only when 'card is mandatory' based on txn type and amounts
    public static final int TXN_IMAGE_CAPTURE_CARD_REQUIRED = 1;
    public static final int TXN_IMAGE_CAPTURE_NEVER = 2;

    // 'value_datatype' column values
    private static final int DATATYPE_INT = 1;
    private static final int DATATYPE_BOOLEAN = 2;
    private static final int DATATYPE_STRING = 3;
    private static final int DATATYPE_DATE = 4;

    public static void initSync(RunMode runMode) {
        mRunMode = runMode;
        if(mRunMode==RunMode.backend) {
            // all from defined constants - rather than DB
            return;
        }

        if(mSettings==null) {
            mSettings = new TreeMap<>();
        }
        //try {
        BackendlessCollection<GlobalSettings> settings = Backendless.Persistence.of( GlobalSettings.class).find();

        // create a map with 'name' as key and 'related value column' as value
        int cnt = settings.getTotalObjects();
        if(cnt > 0) {
            while (settings.getCurrentPage().size() > 0)
            {
                Iterator<GlobalSettings> iterator = settings.getCurrentPage().iterator();
                while( iterator.hasNext() )
                {
                    GlobalSettings setting = iterator.next();

                    Object value = null;
                    switch(setting.getValue_datatype()) {
                        case DATATYPE_INT:
                            value = setting.getValue_int();
                            break;
                        case DATATYPE_BOOLEAN:
                            value = (setting.getValue_int()>0);
                            break;
                        case DATATYPE_STRING:
                            value = setting.getValue_string();
                            break;
                        case DATATYPE_DATE:
                            value = setting.getValue_date();
                            break;
                    }

                    if(value != null) {
                        mSettings.put(setting.getName(), value);

                        // store only user visible settings in the list
                        if(setting.getUser_visible()) {
                            gSettings gSetting = new gSettings();
                            gSetting.name = setting.getDisplay_name();
                            gSetting.description = setting.getDescription();

                            if(setting.getUpdated()==null) {
                                gSetting.updated = setting.getCreated();
                            } else {
                                gSetting.updated = setting.getUpdated();
                            }

                            if(setting.getValue_datatype()!=DATATYPE_DATE) {
                                gSetting.value = value.toString();
                            } else {
                                SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                                gSetting.value = mSdfDateWithTime.format(setting.getValue_date());
                            }
                            userVisibleSettings.add(gSetting);
                        }
                    }
                }
                settings = settings.nextPage();
            }
            //LogMy.d(TAG, "Fetched global settings: "+mSettings.size());
        } else {
                /*LogMy.e(TAG, "Failed to fetch global settings.");
                AppAlarms.noDataAvailable("",DbConstants.USER_TYPE_MERCHANT,"MyGlobalSettings-initSync",null);
                return ErrorCodes.GENERAL_ERROR;*/
        }
        /*} catch (BackendlessException e) {
            LogMy.e(TAG,"Failed to fetch global settings: "+e.toString());
            AppAlarms.handleException(e);
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;*/
    }

}
