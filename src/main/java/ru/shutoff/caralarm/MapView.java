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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapView extends WebViewActivity {

    SharedPreferences preferences;
    String track;
    BroadcastReceiver br;

    static final String WAYS_URL = "http://api.car-online.ru/v2?get=waystands&skey=$1&begin=$2&end=$3&content=json";
    static final String GPS_URL = "http://api.car-online.ru/v2?get=gpslist&skey=$1&begin=$2&end=$3&content=json";

    class JsInterface {
        @JavascriptInterface
        public double getLatitude() {
            return Double.parseDouble(preferences.getString(Names.Latitude, "0"));
        }

        @JavascriptInterface
        public double getLongitude() {
            return Double.parseDouble(preferences.getString(Names.Longitude, "0"));
        }

        @JavascriptInterface
        public String getTrack() {
            return track;
        }

        @JavascriptInterface
        public void ready() {
            HttpTask httpTask = new HttpTask() {
                @Override
                void result(JSONObject res) throws JSONException {
                    if (res == null)
                        return;
                    JSONArray list = res.getJSONArray("waystandlist");
                    int last = list.length() - 1;
                    JSONObject way = list.getJSONObject(last);
                    String type = way.getString("type");
                    if (!type.equals("WAY"))
                        return;
                    list = way.getJSONArray("events");
                    last = list.length() - 1;
                    String begin = list.getJSONObject(0).getString("eventTime");
                    String end = list.getJSONObject(last).getString("eventTime");
                    HttpTask gpsTask = new HttpTask() {
                        @Override
                        void result(JSONObject res) throws JSONException {
                            if (res == null)
                                return;
                            track = null;
                            JSONArray list = res.getJSONArray("gpslist");
                            for (int i = 0; i < list.length(); i++){
                                JSONObject gps = list.getJSONObject(i);
                                if (!gps.getBoolean("valid"))
                                    return;
                                if (track != null){
                                    track += "|";
                                }else{
                                    track = "";
                                }
                                track += gps.getString("latitude") + "," + gps.getString("longitude");
                            }
                            if (track != null){
                                webView.loadUrl("javascript:setTrack()");
                            }
                        }

                        @Override
                        void error() {
                            // ignore
                        }
                    };
                    gpsTask.execute(GPS_URL,
                        preferences.getString(Names.KEY, ""),
                        begin, end);
                }

                @Override
                void error() {
                    // ignore
                }
            };
            long lastTime = preferences.getLong(Names.EventTime, 0);
            httpTask.execute(WAYS_URL,
                preferences.getString(Names.KEY, ""),
                (lastTime - 24 * 60 * 60 * 1000) + "",
                lastTime + "");
        }

    }

    @Override
    String loadURL() {
        webView.addJavascriptInterface(new JsInterface(), "android");
        String map_type = preferences.getString(Names.MAP_TYPE, "");
        if (map_type.equals("Yandex"))
            return "file:///android_asset/html/ymaps.html";
        return "file:///android_asset/html/maps.html";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                webView.loadUrl("javascript:update()");
            }
        };
        registerReceiver(br, new IntentFilter(StatusService.ACTION_UPDATE));
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
