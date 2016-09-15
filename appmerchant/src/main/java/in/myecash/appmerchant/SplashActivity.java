package in.myecash.appmerchant;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.backendless.Backendless;
import com.crashlytics.android.Crashlytics;
import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.BackendSettings;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.entities.MyGlobalSettings;
import in.myecash.commonbase.models.Address;
import in.myecash.commonbase.models.GlobalSettings;
import in.myecash.commonbase.models.MerchantDevice;
import in.myecash.commonbase.models.Merchants;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;

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
        Backendless.Data.mapTableToClass("Address", Address.class);
        Backendless.Data.mapTableToClass("Merchants", Merchants.class);
        Backendless.Data.mapTableToClass("MerchantDevice", MerchantDevice.class);
        Backendless.Data.mapTableToClass("GlobalSettings", GlobalSettings.class);

        if(savedInstanceState==null) {
            int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(SplashActivity.this);
            if ( resultCode != ErrorCodes.NO_ERROR) {
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, ErrorCodes.appErrorDesc.get(resultCode), false, true)
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
            return MyGlobalSettings.initSync();
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
                    String errorStr = ErrorCodes.appErrorDesc.get(ErrorCodes.SERVICE_GLOBAL_DISABLED)
                            + mSdfDateWithTime.format(disabledUntil);
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errorStr, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            } else {
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }
}
