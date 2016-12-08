package in.myecash.appagent;

import android.app.Application;

import com.backendless.Backendless;
import com.crashlytics.android.Crashlytics;
import com.helpshift.All;
import com.helpshift.Core;
import com.helpshift.InstallConfig;
import com.helpshift.exceptions.InstallException;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.constants.CommonConstants;
import io.fabric.sdk.android.Fabric;

/**
 * Created by adgangwa on 09-12-2016.
 */
public class AgentApp extends Application {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        // Required initialization logic here!

        // Init crashlytics
        Fabric.with(this, new Crashlytics());

        // Helpshift Init - For Help section in Merchants App
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

        // App level initializations - once in main activity
        Backendless.initApp(this, CommonConstants.APPLICATION_ID, CommonConstants.ANDROID_SECRET_KEY, CommonConstants.VERSION);
        com.backendless.Backendless.setUrl( CommonConstants.BACKENDLESS_HOST );

        // Map all tables to class here - except 'cashback' and 'transaction'
        AppCommonUtil.initTableToClassMappings();
    }
}

