package in.myecash.appmerchant;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.backendless.Backendless;
import com.backendless.exceptions.BackendlessException;
import com.crashlytics.android.Crashlytics;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.constants.BackendSettings;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * Created by adgangwa on 27-04-2016.
 */
public class SplashActivity extends AppCompatActivity
        implements DialogFragmentWrapper.DialogFragmentWrapperIf {
    private static final String TAG = "SplashActivity";

    private FetchGlobalSettings mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Init crashlytics
        //CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_splash);

        // App level initializations - once in main activity
        Backendless.initApp(this, BackendSettings.APPLICATION_ID, BackendSettings.ANDROID_SECRET_KEY, BackendSettings.VERSION);
        com.backendless.Backendless.setUrl( BackendSettings.BACKENDLESS_HOST );

        // Map all tables to class here - except 'cashback' and 'transaction'
        AppCommonUtil.initTableToClassMappings();

        if(savedInstanceState==null) {
            int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(SplashActivity.this);
            if ( resultCode != ErrorCodes.NO_ERROR) {
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            } else {
                AppCommonUtil.showProgressDialog(this, "Loading ...");
                mTask = new FetchGlobalSettings();
                mTask.execute();
            }
        }
    }

    @Override
    public void onDestroy() {
        if(mTask!=null) {
            mTask.cancel(true);
            mTask = null;
            LogMy.d(TAG, "Background thread destroyed");
        }
        super.onDestroy();
    }

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

    private class FetchGlobalSettings extends AsyncTask<Void,Void,Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            //return MyGlobalSettings.initSync();
            try {
                MyGlobalSettings.initSync(MyGlobalSettings.RunMode.appMerchant);
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
            AppCommonUtil.cancelProgressDialog(true);
            if(errorCode==ErrorCodes.NO_ERROR) {
                Date disabledUntil = MyGlobalSettings.getServiceDisabledUntil();
                if(disabledUntil == null) {
                    startLoginActivity();
                } else {
                    // Add time at the end of error message
                    SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                    String errorStr = AppCommonUtil.getErrorDesc(ErrorCodes.SERVICE_GLOBAL_DISABLED)
                            + mSdfDateWithTime.format(disabledUntil);
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errorStr, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            } else {
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }
}
