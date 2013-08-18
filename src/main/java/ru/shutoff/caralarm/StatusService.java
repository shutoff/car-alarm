package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusService extends Service {

    final long REPEAT_AFTER_ERROR = 20 * 1000;

    BroadcastReceiver mReceiver;
    PendingIntent pi;

    SharedPreferences preferences;
    ConnectivityManager conMgr;
    PowerManager powerMgr;
    AlarmManager alarmMgr;

    static final String ACTION_UPDATE = "ru.shutoff.caralarm.UPDATE";

    static final Pattern balancePattern = Pattern.compile("^-?[0-9]+\\.[0-9][0-9]");

    static final String STATUS_URL = "http://api.car-online.ru/v2?get=lastinfo&skey=$1&content=json";
    static final String TEMP_URL = "http://api.car-online.ru/v2?get=temperaturelist&skey=$1&begin=$2&end=$3&content=json";

    HttpTask statusRequest;
    HttpTask temperatureRequest;

    String api_key;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        State.setExceptionHandler();
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getService(this, 0, new Intent(this, StatusService.class), 0);
        mReceiver = new ScreenReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRequest();
        return START_STICKY;
    }

    void startRequest() {
        if (statusRequest != null)
            return;
        api_key = preferences.getString(Names.KEY, "");
        if (api_key.length() == 0)
            return;
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if ((activeNetwork == null) || !activeNetwork.isConnected()) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(mReceiver, filter);
            return;
        }
        if (!powerMgr.isScreenOn()) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            registerReceiver(mReceiver, filter);
            return;
        }
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // ignore
        }
        alarmMgr.cancel(pi);
        statusRequest = new HttpTask() {

            @Override
            void result(JSONObject res) throws JSONException {
                statusRequest = null;
                JSONObject event = res.getJSONObject("event");
                long eventId = event.getLong("eventId");
                if (eventId == preferences.getLong(Names.EventId, 0))
                    return;
                long eventTime = event.getLong("eventTime");
                SharedPreferences.Editor ed = preferences.edit();
                ed.putLong(Names.EventId, eventId);
                ed.putLong(Names.EventTime, eventTime);

                JSONObject voltage = res.getJSONObject("voltage");
                ed.putString(Names.VoltageMain, voltage.getString("main"));
                ed.putString(Names.VoltageReserved, voltage.getString("reserved"));

                JSONObject balance = res.getJSONObject("balance");
                Matcher m = balancePattern.matcher(balance.getString("source"));
                if (m.find())
                    ed.putString(Names.Balance, m.group(0));

                JSONObject gps = res.getJSONObject("gps");
                ed.putString(Names.Latitude, gps.getString("latitude"));
                ed.putString(Names.Longitude, gps.getString("longitude"));
                ed.putString(Names.Speed, gps.getString("speed"));

                JSONObject contact = res.getJSONObject("contact");
                ed.putBoolean(Names.Guard, contact.getBoolean("stGuard"));
                ed.putBoolean(Names.Input1, contact.getBoolean("stInput1"));
                ed.putBoolean(Names.Input2, contact.getBoolean("stInput2"));
                ed.putBoolean(Names.Input3, contact.getBoolean("stInput3"));
                ed.putBoolean(Names.Input4, contact.getBoolean("stInput4"));
                ed.putBoolean(Names.ZoneDoor, contact.getBoolean("stZoneDoor"));
                ed.putBoolean(Names.ZoneHood, contact.getBoolean("stZoneHood"));
                ed.putBoolean(Names.ZoneTrunk, contact.getBoolean("stZoneTrunk"));
                ed.putBoolean(Names.ZoneAccessory, contact.getBoolean("stZoneAccessoryOn"));
                ed.putBoolean(Names.ZoneIgnition, contact.getBoolean("stZoneIgnitionOn"));

                ed.commit();
                sendUpdate();

                if (temperatureRequest != null)
                    return;

                temperatureRequest = new HttpTask() {
                    @Override
                    void result(JSONObject res) throws JSONException {
                        temperatureRequest = null;
                        if (res == null)
                            return;
                        JSONArray arr = res.getJSONArray("temperatureList");
                        JSONObject value = arr.getJSONObject(0);
                        String temp = value.getString("value");
                        if (temp.equals(preferences.getString(Names.Temperature, "")))
                            return;
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.Temperature, temp);
                        ed.commit();
                        sendUpdate();
                    }

                    @Override
                    void error() {
                        // ignore
                    }
                };
                temperatureRequest.execute(TEMP_URL, api_key,
                        (eventTime - 24 * 60 * 60 * 1000) + "",
                        eventTime + "");

            }

            @Override
            void error() {
                alarmMgr.setInexactRepeating(AlarmManager.RTC,
                        System.currentTimeMillis() + REPEAT_AFTER_ERROR, REPEAT_AFTER_ERROR, pi);
            }
        };

        statusRequest.execute(STATUS_URL, api_key);
    }

    void sendUpdate() {
        try {
            Intent intent = new Intent(ACTION_UPDATE);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

}
