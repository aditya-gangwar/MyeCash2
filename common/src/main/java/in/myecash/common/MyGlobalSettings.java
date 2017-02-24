package in.myecash.common;

/**
 * Created by adgangwa on 19-02-2016.
 */

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.GlobalSettingConstants;
import in.myecash.common.database.GlobalSettings;

import java.text.SimpleDateFormat;
import java.util.*;

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
    public static RunMode getRunMode() {
        return mRunMode;
    }
    public static void setRunMode(RunMode mRunMode) {
        MyGlobalSettings.mRunMode = mRunMode;
    }

    public static Map<String,Object> mSettings;
    public static List<gSettings> userVisibleSettings;

    public static class gSettings {
        public String name;
        public String value;
        public String description;
        public Date updated;
    }

    public static boolean isAvailable() {
        return mSettings!=null;
    }

    /*
     * Access Functions
     */
    public static Integer getMchntPasswdResetMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_MERCHANT_PASSWD_RESET_MINS);
    }
    public static Integer getCustPasswdResetMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUSTOMER_PASSWD_RESET_MINS);
    }
    public static Integer getAccBlockMins(Integer userType) {
        if(userType==DbConstants.USER_TYPE_CUSTOMER) {
            return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUSTOMER_ACCOUNT_BLOCK_MINS);
        } else {
            return (Integer) getValue(GlobalSettingConstants.SETTINGS_MERCHANT_ACCOUNT_BLOCK_MINS);
        }
    }
    public static Boolean getCardReqCbRedeem() {
        return (Boolean) getValue(GlobalSettingConstants.SETTINGS_CB_REDEEM_CARD_REQ);
    }
    public static Boolean getCardReqAccDebit() {
        return (Boolean) getValue(GlobalSettingConstants.SETTINGS_ACC_DB_CARD_REQ);
    }
    public static Integer getAccAddPinLimit() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CL_CREDIT_LIMIT_FOR_PIN);
    }
    public static Integer getAccDebitPinLimit() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CL_DEBIT_LIMIT_FOR_PIN);
    }
    public static Integer getCbDebitPinLimit() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CB_DEBIT_LIMIT_FOR_PIN);
    }
    public static Integer getMchntDashBNoRefreshMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_STATS_NO_REFRESH_MINS);
    }
    public static Integer getCustNoRefreshMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUSTOMER_NO_REFRESH_MINS);
    }
    public static Integer getCashAccLimit() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUSTOMER_CASH_LIMIT);
    }
    public static Integer getMchntExpiryDays() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS);
    }
    public static Integer getCustAccLimitModeMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUST_ACC_LIMIT_MODE_MINS);
    }
    public static Integer getWrongAttemptResetMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_WRONG_ATTEMPT_RESET_MINS);
    }
    public static Integer getTxnsIntableKeepDays() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_TXNS_INTABLE_KEEP_DAYS);
    }
    public static Integer getOpsKeepDays() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_OPS_KEEP_DAYS);
    }
    public static Integer getOtpValidMins() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_OTP_VALID_MINS);
    }
    public static Integer getWrongAttemptLimit(int userType) {
        if(userType==DbConstants.USER_TYPE_CUSTOMER) {
            return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUSTOMER_WRONG_ATTEMPT_LIMIT);
        } else {
            return (Integer) getValue(GlobalSettingConstants.SETTINGS_MERCHANT_WRONG_ATTEMPT_LIMIT);
        }
    }
    public static Integer getMchntRenewalDuration() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_MCHNT_RENEW_DURATION);
    }
    public static Integer getCustRenewalDuration() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUST_RENEW_DURATION);
    }
    public static Integer getMchntTxnHistoryDays() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_MCHNT_TXN_HISTORY_DAYS);
    }
    public static Integer getCustTxnHistoryDays() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CUST_TXN_HISTORY_DAYS);
    }
    public static Integer getCbRedeemLimit() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_CB_REDEEM_LIMIT);
    }
    public static Date getServiceDisabledUntil() {
        return (Date) getValue(GlobalSettingConstants.SETTINGS_SERVICE_DISABLED_UNTIL);
    }
    public static Integer getCardImageCaptureMode() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_TXN_IMAGE_CAPTURE_MODE);
    }
    public static Integer getDailyDownStartHour() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_DAILY_DOWNTIME_START_HOUR);
    }
    public static Integer getDailyDownEndHour() {
        return (Integer) getValue(GlobalSettingConstants.SETTINGS_DAILY_DOWNTIME_END_HOUR);
    }
    public static String getServiceNAUrl() {
        return (String) getValue(GlobalSettingConstants.SETTINGS_SERVICE_NA_URL);
    }
    public static String getTermsUrl() {
        if(getRunMode()== MyGlobalSettings.RunMode.appCustomer) {
            return (String) getValue(GlobalSettingConstants.SETTINGS_CUST_TERMS_URL);
        } else {
            return (String) getValue(GlobalSettingConstants.SETTINGS_MCHNT_TERMS_URL);
        }
    }

    private static Object getValue(String gSettingKey) {
        if(mRunMode==RunMode.backend) {
            return getConstantValue(gSettingKey);
        } else {
            if(mSettings==null) {
                // for some reason global settings not available - handle gracefully - return local configured value
                return getConstantValue(gSettingKey);
            } else {
                return mSettings.get(gSettingKey);
            }
        }
    }

    private static Object getConstantValue(String gSettingKey) {
        String value = GlobalSettingConstants.valuesGlobalSettings.get(gSettingKey);
        Integer valueType = GlobalSettingConstants.valueTypesGlobalSettings.get(gSettingKey);
        switch (valueType) {
            case GlobalSettingConstants.DATATYPE_INT:
                return Integer.parseInt(value);
            case GlobalSettingConstants.DATATYPE_BOOLEAN:
                return Boolean.parseBoolean(value);
            case GlobalSettingConstants.DATATYPE_STRING:
                return value;
            case GlobalSettingConstants.DATATYPE_DATE:
                long epochTime = Long.parseLong(value);
                return new Date(epochTime);
            default:
                return mSettings.get(gSettingKey);
        }
    }

    public static void initSync(RunMode runMode) {
        mRunMode = runMode;
        if(mRunMode==RunMode.backend) {
            // all from defined constants - rather than DB
            return;
        }

        if(mSettings==null) {
            mSettings = new TreeMap<>();
        } else {
            mSettings.clear();
        }

        if(userVisibleSettings==null) {
            userVisibleSettings = new ArrayList<>();
        } else {
            userVisibleSettings.clear();
        }

        //try {
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.addSortByOption("name ASC");
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setQueryOptions( queryOptions );


        //BackendlessCollection<GlobalSettings> settings = Backendless.Persistence.of( GlobalSettings.class).find();
        BackendlessCollection<GlobalSettings> settings = Backendless.Data.of(GlobalSettings.class).find(dataQuery);

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
                        case GlobalSettingConstants.DATATYPE_INT:
                            value = setting.getValue_int();
                            break;
                        case GlobalSettingConstants.DATATYPE_BOOLEAN:
                            value = (setting.getValue_int()>0);
                            break;
                        case GlobalSettingConstants.DATATYPE_STRING:
                            value = setting.getValue_string();
                            break;
                        case GlobalSettingConstants.DATATYPE_DATE:
                            value = setting.getValue_date();
                            break;
                    }

                    if(value != null) {
                        mSettings.put(setting.getName(), value);

                        // store only user visible settings in the list
                        if(setting.getUser_visible()) {
                            gSettings gSetting = new gSettings();
                            gSetting.name = setting.getName();
                            gSetting.description = setting.getDescription();

                            if(setting.getUpdated()==null) {
                                gSetting.updated = setting.getCreated();
                            } else {
                                gSetting.updated = setting.getUpdated();
                            }

                            if(setting.getValue_datatype()!=GlobalSettingConstants.DATATYPE_DATE) {
                                gSetting.value = value.toString();
                            } else {
                                SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                                mSdfDateWithTime.setTimeZone(TimeZone.getTimeZone(CommonConstants.TIMEZONE));
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
