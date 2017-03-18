package in.myecash.merchant;

import android.app.Application;
import android.os.StrictMode;

import com.backendless.Backendless;
import com.crashlytics.android.Crashlytics;
import com.helpshift.All;
import com.helpshift.Core;
import com.helpshift.InstallConfig;
import com.helpshift.exceptions.InstallException;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.MyGlobalSettings;
import io.fabric.sdk.android.Fabric;

/**
 * Created by adgangwa on 08-12-2016.
 */
public class MerchantApp extends Application {
    private static final String TAG = "MchntApp-Application";

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        // Required initialization logic here!
        // Init crashlytics
        //CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        if(AppConstants.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
        }

        // Helpshift Init
        InstallConfig installConfig = new InstallConfig.Builder()
                .setNotificationIcon(R.drawable.logo_blue)
                .build();
        Core.init(All.getInstance());
        try {
            Core.install(this,
                    "edd7afe788e184aa8c12d8aa278fb467",
                    "myecash.helpshift.com",
                    "myecash_platform_20161206110224571-d81994b14f0dbad",
                    installConfig);
            
        } catch (InstallException e) {
            LogMy.e("MerchantApp", "Helpshift: Invalid install credentials : ", e);
        }

        LogMy.d(TAG,AppConstants.BACKENDLESS_APP_ID +","+AppConstants.ANDROID_SECRET_KEY+","+AppConstants.VERSION);
        LogMy.d(TAG,AppConstants.BACKENDLESS_HOST+","+AppConstants.USE_CRASHLYTICS);

        // App level initializations - once in main activity
        Backendless.initApp(this, AppConstants.BACKENDLESS_APP_ID, AppConstants.ANDROID_SECRET_KEY, AppConstants.VERSION);
        com.backendless.Backendless.setUrl( AppConstants.BACKENDLESS_HOST );

        // Map all tables to class here - except 'cashback' and 'transaction'
        AppCommonUtil.initTableToClassMappings();
        MyGlobalSettings.setRunMode(MyGlobalSettings.RunMode.appMerchant);

        // This is to avoid android.os.FileUriExposedException exception
        // while trying to pass CSV file path to Email app intent (txn list email)
        // Refer: http://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

    }
}
