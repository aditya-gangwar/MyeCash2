package in.myecash.appbase;

/**
 * Created by adgangwa on 23-02-2017.
 */

import android.app.DownloadManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.MyGlobalSettings;

/**
 * Created by adgangwa on 11-02-2017.
 */

public class SingleWebViewActivity extends AppCompatActivity {

    private static final String TAG = "BaseApp-SingleWebViewActivity";

    // constants used to pass extra data in the intent
    public static final String INTENT_EXTRA_URL = "LoadUrl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_webview);

        try {
            String url = getIntent().getStringExtra(INTENT_EXTRA_URL);
            if(url!=null && !url.isEmpty()) {
                WebView myWebView = (WebView) findViewById(R.id.webview);

                myWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
                myWebView.getSettings().setLoadWithOverviewMode(true);
                myWebView.getSettings().setUseWideViewPort(true);
                myWebView.getSettings().setBuiltInZoomControls(true);

                myWebView.setWebViewClient(new MyWebViewClient());

                myWebView.setDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                        request.setMimeType(mimeType);
                        //------------------------COOKIE!!------------------------
                        String cookies = CookieManager.getInstance().getCookie(url);
                        request.addRequestHeader("cookie", cookies);
                        //------------------------COOKIE!!------------------------
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setDescription("Downloading file...");
                        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                    }
                });

                LogMy.d(TAG,url);
                myWebView.loadUrl(url);
            }

        }catch (Exception e) {
            LogMy.e(TAG,"Exception in Terms Activity",e);
        }
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            AppCommonUtil.showProgressDialog(SingleWebViewActivity.this, "Loading...");
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            AppCommonUtil.cancelProgressDialog(true);
        }
    }
}

