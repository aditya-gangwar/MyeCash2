package in.myecash.commonbase.entities;

/**
 * Created by adgangwa on 19-02-2016.
 */

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;

import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.GlobalSettings;
import in.myecash.commonbase.utilities.AppAlarms;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MyGlobalSettings
{
    private static String TAG="GlobalSettings";
    public static Map<String,Object> mSettings;

    public static List<gSettings> userVisibleSettings = new ArrayList<>();

    public static class gSettings {
        public String name;
        public String value;
        public String description;
        public Date updated;
    }

    public static int initSync() {

        if(mSettings==null) {
            mSettings = new TreeMap<>();
        }
        try {
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
                            case DbConstants.DATATYPE_INT:
                                value = setting.getValue_int();
                                break;
                            case DbConstants.DATATYPE_BOOLEAN:
                                value = (setting.getValue_int()>0);
                                break;
                            case DbConstants.DATATYPE_STRING:
                                value = setting.getValue_string();
                                break;
                            case DbConstants.DATATYPE_DATE:
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

                                if(setting.getValue_datatype()!=DbConstants.DATATYPE_DATE) {
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
                LogMy.d(TAG, "Fetched global settings: "+mSettings.size());
            } else {
                LogMy.e(TAG, "Failed to fetch global settings.");
                AppAlarms.noDataAvailable("",DbConstants.USER_TYPE_MERCHANT,"MyGlobalSettings-initSync",null);
                return ErrorCodes.GENERAL_ERROR;
            }
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Failed to fetch global settings: "+e.toString());
            AppAlarms.handleException(e);
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return ErrorCodes.NO_ERROR;
    }

    public static Integer getMchntPasswdResetMins() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_MERCHANT_PASSWD_RESET_MINS);
    }
    public static Integer getMchntAccBlockHrs() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_MERCHANT_ACCOUNT_BLOCK_HRS);
    }
    public static Integer getMchntDashBNoRefreshHrs() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_DASHBOARD_NO_REFRESH_HRS);
    }
    public static Integer getAccAddPinLimit() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CL_CREDIT_LIMIT_FOR_PIN);
    }
    public static Integer getAccDebitPinLimit() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CL_DEBIT_LIMIT_FOR_PIN);
    }
    public static Integer getCbDebitPinLimit() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CB_DEBIT_LIMIT_FOR_PIN);
    }
    public static Integer getCbRedeemLimit() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CB_REDEEM_LIMIT);
    }
    public static Integer getMchntReportHistoryDays() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_REPORTS_HISTORY_DAYS);
    }
    public static Integer getMchntNoReportStartHrs() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_REPORTS_BLACKOUT_END);
    }
    public static Integer getMchntNoReportEndHrs() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_REPORTS_BLACKOUT_START);
    }
    public static Integer getCustAccBlockHrs() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CUSTOMER_ACCOUNT_BLOCK_HRS);
    }
    public static Integer getCashAccLimit() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CUSTOMER_CASH_LIMIT);
    }
    public static Date getServiceDisabledUntil() {
        return (Date)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_SERVICE_DISABLED_UNTIL);
    }
    public static Integer getCardImageCaptureMode() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_TXN_IMAGE_CAPTURE_MODE);
    }
    public static Boolean getCardReqCbRedeem() {
        return (Boolean)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CB_REDEEM_CARD_REQ);
    }
    public static Boolean getCardReqAccDebit() {
        return (Boolean)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_ACC_DB_CARD_REQ);
    }
    public static Integer getMchntExpiryDays() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_MCHNT_REMOVAL_EXPIRY_DAYS);
    }
    public static Integer getCustPasswdResetMins() {
        return (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_CUSTOMER_PASSWD_RESET_MINS);
    }

    /*
    private static Object getValue(String name) {
        switch(name) {
            case DbConstants.merchant_passwd_reset_mins:
                return
                break;
            case DbConstants.merchant_wrong_password_attempts:
                break;
            case DbConstants.otp_valid_mins:
                break;
            case DbConstants.cl_limit_for_pin_card:
                break;
            case DbConstants.cb_redeem_limit:
                break;
            case DbConstants.cb_limit_for_pin_card:
                break;
            case DbConstants.reports_history_days:
                break;
            case DbConstants.reports_blackout_end:
                break;
            case DbConstants.reports_blackout_start:
                break;
            case DbConstants.service_disabled_until:
                break;
            case DbConstants.customer_account_block_hrs:
                break;
            case DbConstants.customer_wrong_pin_attempts:
                break;
        }
    }
    */

}
