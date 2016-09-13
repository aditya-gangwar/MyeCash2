package in.myecash.commonbase.entities;

/**
 * Created by adgangwa on 19-02-2016.
 */

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.GlobalSettings;
import in.myecash.commonbase.utilities.AppAlarms;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MyGlobalSettings
{
    private static String TAG="GlobalSettings";
    public static Map<String,Object> mSettings;

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
                            case DbConstants.DATATYPE_STRING:
                                value = setting.getValue_string();
                                break;
                            case DbConstants.DATATYPE_DATE:
                                value = setting.getValue_date();
                                break;
                        }
                        if(value != null) {
                            mSettings.put(setting.getName(), value);
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
