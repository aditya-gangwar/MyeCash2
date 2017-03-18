package in.myecash.internal;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;

import java.util.ArrayList;

/**
 * Created by adgangwa on 27-04-2016.
 */
public class SplashActivity extends AppCompatActivity
        implements DialogFragmentWrapper.DialogFragmentWrapperIf {
    private static final String TAG = "AgentApp-SplashActivity";

    //private FetchGlobalSettings mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String naErrorStr = AppCommonUtil.isDownAsPerLocalData(SplashActivity.this);
        if(naErrorStr!=null) {
            DialogFragmentWrapper.createNotification(AppConstants.serviceNATitle, naErrorStr, false, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            startLoginActivity();
        }

        /*int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(SplashActivity.this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            mTask = new FetchGlobalSettings();
            mTask.execute();
        }*/
    }

    /*@Override
    public void onDestroy() {
        if(mTask!=null) {
            mTask.cancel(true);
            mTask = null;
        }
        LogMy.i(TAG, "Background thread destroyed");
        super.onDestroy();
    }*/


    private void startLoginActivity() {

        /*if(MyGlobalSettings.mSettings==null) {
            // I should not be here
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            finish();
        }*/

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        // do nothing
    }

    /*private class FetchGlobalSettings extends AsyncTask<Void,Void,Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                MyGlobalSettings.initSync(MyGlobalSettings.RunMode.appInternalUser);
            } catch (Exception e) {
                LogMy.e(TAG,"Failed to fetch global settings: "+e.toString());
                AppAlarms.handleException(e);
                if(e instanceof BackendlessException) {
                    return AppCommonUtil.getLocalErrorCode((BackendlessException) e);
                }
                return ErrorCodes.GENERAL_ERROR;
            }
            return ErrorCodes.NO_ERROR;
        }

        @Override
        protected void onPostExecute(Integer errorCode) {
            if(errorCode==ErrorCodes.NO_ERROR) {
                // Check for daily downtime
                int startHour = MyGlobalSettings.getDailyDownStartHour();
                int endHour = MyGlobalSettings.getDailyDownEndHour();
                if(endHour > startHour) {
                    int currHour = (new DateUtil()).getHourOfDay();
                    if(currHour >= startHour && currHour < endHour) {
                        // Show error notification dialog
                        String errorStr = "Service is not available daily between "+startHour+":00 and "+endHour+":00 hours.";
                        DialogFragmentWrapper.createNotification(AppConstants.serviceNATitle, errorStr, false, true)
                                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                        return;
                    }
                }

                Date disabledUntil = MyGlobalSettings.getServiceDisabledUntil();
                if(disabledUntil == null || System.currentTimeMillis() > disabledUntil.getTime()) {
                    startLoginActivity();
                } else {

                    // Add time at the end of error message
                    SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                    String errorStr = AppCommonUtil.getErrorDesc(ErrorCodes.SERVICE_GLOBAL_DISABLED) + mSdfDateWithTime.format(disabledUntil);
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errorStr, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            } else {
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }*/
}
