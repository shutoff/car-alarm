package ru.shutoff.caralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;

public class MapView extends WebViewActivity {

    SharedPreferences preferences;
    BroadcastReceiver br;
    String car_id;

    class JsInterface {
        @JavascriptInterface
        public double getLatitude() {
            return Double.parseDouble(preferences.getString(Names.LATITUDE + car_id, "0"));
        }

        @JavascriptInterface
        public double getLongitude() {
            return Double.parseDouble(preferences.getString(Names.LONGITUDE + car_id, "0"));
        }

        @JavascriptInterface
        public String getAddress() {
            String address = preferences.getString(Names.ADDRESS + car_id, "");
            String[] parts = address.split(", ");
            if (parts.length < 3)
                return address;
            address = parts[0] + ", " + parts[1];
            for (int i = 2; i < parts.length; i++)
                address += "<br/>" + parts[i];
            return address;
        }
    }

    @Override
    String loadURL() {
        webView.addJavascriptInterface(new JsInterface(), "android");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("map_type", "").equals("OSM"))
            return "file:///android_asset/html/omaps.html";
        return "file:///android_asset/html/maps.html";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        car_id = Preferences.getCar(preferences, getIntent().getStringExtra(Names.ID));

        super.onCreate(savedInstanceState);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                webView.loadUrl("javascript:update()");
            }
        };
        registerReceiver(br, new IntentFilter(FetchService.ACTION_UPDATE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map: {
                webView.loadUrl("javascript:center()");
                break;
            }
        }
        return false;
    }
}
