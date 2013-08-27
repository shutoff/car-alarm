package ru.shutoff.caralarm;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

abstract public class WebViewActivity extends ActionBarActivity {

    abstract String loadURL();

    WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        webView = (WebView) findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        WebChromeClient mChromeClient = new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                // TODO Auto-generated method stub
                Log.v("ChromeClient", "invoked: onConsoleMessage() - " + sourceID + ":"
                        + lineNumber + " - " + message);
                super.onConsoleMessage(message, lineNumber, sourceID);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.v("ChromeClient", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        };
        webView.setWebChromeClient(mChromeClient);

        webView.loadUrl(loadURL());
    }

}
