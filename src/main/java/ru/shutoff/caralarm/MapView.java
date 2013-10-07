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

import org.joda.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MapView extends WebViewActivity {

    SharedPreferences preferences;
    BroadcastReceiver br;
    String car_id;

    String[] car_data;

    final static String WAYSTANDS = "http://api.car-online.ru/v2?get=waystands&skey=$1&begin=$2&content=json";

    class JsInterface {

        @JavascriptInterface
        public String getData() {
            String first = null;
            String last = null;
            for (String data : car_data) {
                String[] p = data.split(";");
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

        if (savedInstanceState != null) {
            car_data = savedInstanceState.getStringArray(Names.CARS);
        } else {
            Cars.Car[] cars = Cars.getCars(this);
            car_data = new String[cars.length];
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
                    String address = Address.getAddress(preferences, id);
                    String[] parts = address.split(", ");
                    if (parts.length >= 3) {
                        address = parts[0] + ", " + parts[1];
                        for (int n = 2; n < parts.length; n++)
                            address += "<br/>" + parts[n];
                    }
                    data += address;
                }
                car_data[i] = data;
                new StateRequest(id);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(Names.CARS, car_data);
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

    class StateRequest extends HttpTask {

        String car_id;

        StateRequest(String id) {
            car_id = id;
            Date now = new Date();
            execute(WAYSTANDS, preferences.getString(Names.CAR_KEY + car_id, ""), (now.getTime() - 24 * 60 * 60 * 1000) + "");
        }

        @Override
        void result(JSONObject res) throws JSONException {
            JSONArray list = res.getJSONArray("waystandlist");
            if (list.length() > 1) {
                JSONObject last = list.getJSONObject(list.length() - 1);
                String type = last.getString("type");
                String speed = null;
                JSONArray events = last.getJSONArray("events");
                JSONObject event;
                if (type.equals("WAY")) {
                    event = events.getJSONObject(events.length() - 1);
                    double s = Double.parseDouble(preferences.getString(Names.SPEED + car_id, "0"));
                    speed = String.format(getString(R.string.speed), s);
                } else {
                    event = events.getJSONObject(0);
                }
                LocalDateTime time = new LocalDateTime(event.getLong("eventTime"));
                for (int i = 0; i < car_data.length; i++) {
                    String[] d = car_data[i].split(";");
                    if (d[0].equals(car_id)) {
                        String data = d[0] + ";" + d[1] + ";" + d[2] + ";" + d[3] + ";" + time.toString("HH:mm");
                        if (speed != null)
                            data += ";" + speed;
                        car_data[i] = data;
                        webView.loadUrl("javascript:update()");
                        break;
                    }
                }
            }
        }

        @Override
        void error() {

        }
    }
}
