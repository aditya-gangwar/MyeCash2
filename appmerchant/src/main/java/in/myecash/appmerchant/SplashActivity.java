package in.myecash.appmerchant;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.crashlytics.android.Crashlytics;
import in.myecash.appbase.constants.AppConstants;

import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.RootUtil;
import in.myecash.common.DateUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import in.myecash.merchantbase.LoginActivity;
import io.fabric.sdk.android.Fabric;

/**
 * Created by adgangwa on 27-04-2016.
 */
public class SplashActivity extends AppCompatActivity
        implements DialogFragmentWrapper.DialogFragmentWrapperIf {
    private static final String TAG = "MchntApp-SplashActivity";

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

        /*if(savedInstanceState==null) {
            String naErrorStr = AppCommonUtil.isDownAsPerLocalData(SplashActivity.this);
            if(naErrorStr!=null) {
                DialogFragmentWrapper.createNotification(AppConstants.serviceNATitle, naErrorStr, false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            } else {
                int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(SplashActivity.this);
                if (resultCode != ErrorCodes.NO_ERROR) {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    AppCommonUtil.showProgressDialog(this, "Loading ...");
                    mTask = new FetchGlobalSettings();
                    mTask.execute();
                }
            }
        }*/
    }

    /*@Override
    public void onDestroy() {
        if(mTask!=null) {
            mTask.cancel(true);
            mTask = null;
            LogMy.d(TAG, "Background thread destroyed");
        }
        super.onDestroy();
    }*/

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        if(tag.equals(DialogFragmentWrapper.DIALOG_NOTIFICATION)) {
            finish();
        }
    }

    /*private class FetchGlobalSettings extends AsyncTask<Void,Void,Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            return AppCommonUtil.loadGlobalSettings(MyGlobalSettings.RunMode.appMerchant);
        }

        @Override
        protected void onPostExecute(Integer errorCode) {
            AppCommonUtil.cancelProgressDialog(true);

            if(errorCode==ErrorCodes.NO_ERROR) {
                AppCommonUtil.storeGSLocally(SplashActivity.this);
                startLoginActivity();

            } else if (errorCode == ErrorCodes.SERVICE_GLOBAL_DISABLED) {
                // Add time at the end of error message
                Date disabledUntil = MyGlobalSettings.getServiceDisabledUntil();
                SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                String errorStr = AppCommonUtil.getErrorDesc(ErrorCodes.SERVICE_GLOBAL_DISABLED)
                        + mSdfDateWithTime.format(disabledUntil);
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errorStr, false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            } else if (errorCode == ErrorCodes.UNDER_DAILY_DOWNTIME) {
                // Show error notification dialog
                String errorStr = String.format(AppCommonUtil.getErrorDesc(ErrorCodes.UNDER_DAILY_DOWNTIME),
                        MyGlobalSettings.getDailyDownStartHour(),
                        MyGlobalSettings.getDailyDownEndHour());

                DialogFragmentWrapper.createNotification(AppConstants.serviceNATitle, errorStr, false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            } else if (errorCode == ErrorCodes.REMOTE_SERVICE_NOT_AVAILABLE) {
                // Redirect to webpage
                String url = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this)
                        .getString(AppConstants.PREF_SERVICE_NA_URL, getString(R.string.serviceNaDefaultUrl));
                Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                startActivity(viewIntent);
            } else {
                String errorDesc = AppCommonUtil.getErrorDesc(errorCode);
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errorDesc, false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }*/
}
