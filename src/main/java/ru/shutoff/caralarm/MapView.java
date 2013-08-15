package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class MapView extends Activity {

    WebView mapView;
    TextView tvAddress;
    SharedPreferences preferences;

    class JsInterface {
        @JavascriptInterface
        public String getLatitude()      {
            return preferences.getString(Names.LATITUDE, "0");
        }
        @JavascriptInterface
        public String getLongitude()      {
            return preferences.getString(Names.LONGITUDE, "0");
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mapView = (WebView)findViewById(R.id.webview);
        tvAddress = (TextView) findViewById(R.id.address);
        WebSettings settings = mapView.getSettings();
        settings.setJavaScriptEnabled(true);
        mapView.addJavascriptInterface(new JsInterface(), "android");
        mapView.loadUrl("file:///android_asset/html/maps.html");
        String address = preferences.getString(Names.LATITUDE, "") + " " +
                preferences.getString(Names.LONGITUDE, "") + "\n" +
                preferences.getString(Names.ADDRESS, "");
        tvAddress.setText(address);
        tvAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.loadUrl("javascript:center()");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
