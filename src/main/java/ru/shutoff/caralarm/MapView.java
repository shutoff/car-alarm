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

import java.util.HashMap;
import java.util.Map;

public class MapView extends WebViewActivity {

    SharedPreferences preferences;
    BroadcastReceiver br;
    String car_id;
    Map<String, String> times;

    class JsInterface {

        @JavascriptInterface
        public String getData() {

            Cars.Car[] cars = Cars.getCars(getBaseContext());
            String[] car_data = new String[cars.length];
            for (int i = 0; i < cars.length; i++) {
                String id = cars[i].id;
                String data = id + ";" +
                        preferences.getString(Names.LATITUDE + id, "0") + ";" +
                        preferences.getString(Names.LONGITUDE + id, "0") + ";";
                if (cars.length > 1) {
                    String name = preferences.getString(Names.CAR_NAME + id, "");
                    if (name.length() == 0) {
                        name = getString(R.string.car);
                        if (id.length() > 0)
                            name += " " + id;
                    }
                    data += name + "<br/>";
                    String address = Address.getAddress(getBaseContext(), id);
                    String[] parts = address.split(", ");
                    if (parts.length >= 3) {
                        address = parts[0] + ", " + parts[1];
                        for (int n = 2; n < parts.length; n++)
                            address += "<br/>" + parts[n];
                    }
                    data += address;
                }
                car_data[i] = data;
            }

            String first = null;
            String last = null;
            for (String data : car_data) {
                String[] p = data.split(";");
                if (times.containsKey(p[0]))
                    data += ";" + times.get(p[0]);
                if (p[0].equals(car_id)) {
                    first = data;
                } else {
                    if (last == null) {
                        last = data;
                    } else {
                        last += "|" + data;
                    }
                }
            }
            if (last != null)
                first += "|" + last;
            return first;
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
        times = new HashMap<String, String>();
        if (savedInstanceState != null) {
            String car_data = savedInstanceState.getString(Names.CARS);
            if (car_data != null) {
                String[] data = car_data.split("|");
                for (String d : data) {
                    String[] p = d.split(";");
                    times.put(p[0], p[1]);
                }
            }
        }

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String data = null;
        for (Map.Entry<String, String> v : times.entrySet()) {
            String p = v.getKey() + ";" + v.getValue();
            if (data == null) {
                data = p;
            } else {
                data += "|" + p;
            }
        }
        if (data != null)
            outState.putString(Names.CARS, data);
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
