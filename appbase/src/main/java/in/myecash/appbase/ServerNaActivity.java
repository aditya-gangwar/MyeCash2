package in.myecash.appbase;

/**
 * Created by adgangwa on 11-02-2017.
 */

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import in.myecash.appbase.R;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.MyGlobalSettings;

/**
 * Created by adgangwa on 11-02-2017.
 */

public class ServerNaActivity extends AppCompatActivity {

    private static final String TAG = "BaseApp-ServerNaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_server_na);
        WebView myWebView = (WebView) findViewById(R.id.webview);

        String url = null;
        if(MyGlobalSettings.isAvailable()) {
            url = MyGlobalSettings.getServiceNAUrl();
        } else {
            // MyGlobalSettings.getServiceNAUrl() will use constant value
            url = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(AppConstants.PREF_SERVICE_NA_URL, MyGlobalSettings.getServiceNAUrl());
        }

        LogMy.d(TAG,"Loading : "+url);
        myWebView.loadUrl(url);
    }
}