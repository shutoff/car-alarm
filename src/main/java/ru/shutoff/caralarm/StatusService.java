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
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusService extends Service {

    boolean process_request;

    final long REPEAT_AFTER_ERROR = 20 * 1000;

    BroadcastReceiver mReceiver;
    PendingIntent pi;

    ConnectivityManager conMgr;
    PowerManager powerMgr;
    AlarmManager alarmMgr;

    static final String ACTION_UPDATE = "ru.shutoff.caralarm.UPDATE";

    static final Pattern balancePattern = Pattern.compile("^-?[0-9]+\\.[0-9][0-9]");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.appendLog("Error thread: " + thread.toString());
                State.appendLog("Error: " + ex.toString());
            }
        });
        super.onCreate();
        process_request = false;
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

    abstract class HttpRequest {

        abstract void process(JSONObject result, HttpTask task);

        abstract boolean postProcess(SharedPreferences.Editor ed);

        String url_;
    }

    class HttpTask extends AsyncTask<Void, Void, Void> {

        List<HttpRequest> requests_;
        String error_;

        HttpTask(HttpRequest request) {
            requests_ = new Vector<HttpRequest>();
            requests_.add(request);
            execute();
        }

        void add(HttpRequest request) {
            requests_.add(request);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            HttpClient httpclient = new DefaultHttpClient();
            int nRequest = 0;
            while (nRequest < requests_.size()) {
                HttpRequest req = requests_.get(nRequest);
                try {
                    String url = req.url_;
                    State.appendLog("GET " + url);
                    HttpResponse response = httpclient.execute(new HttpGet(url));
                    StatusLine statusLine = response.getStatusLine();
                    int status = statusLine.getStatusCode();
                    if (status != HttpStatus.SC_OK)
                        return null;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    String res = out.toString();
                    req.process(new JSONObject(res), this);
                } catch (Exception e) {
                    error_ = e.getMessage();
                    State.appendLog("Err: " + error_);
                }
                nRequest++;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor ed = preferences.edit();
            boolean res = false;
            for (HttpRequest req : requests_) {
                res |= req.postProcess(ed);
            }
            process_request = false;
            if (res) {
                State.appendLog("Send update");
                ed.commit();
                sendUpdate();
            }
            if (error_ != null) {
                State.appendLog("Restart request");
                alarmMgr.setInexactRepeating(AlarmManager.RTC,
                        System.currentTimeMillis() + REPEAT_AFTER_ERROR, REPEAT_AFTER_ERROR, pi);
            }
        }
    }

    void sendUpdate() {
        try {
            Intent intent = new Intent(ACTION_UPDATE);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    class LocationRequest extends HttpRequest {

        LocationRequest(String longitude, String latitude) {
            url_ = "http://maps.googleapis.com/maps/api/geocode/json?latlng=";
            url_ += latitude;
            url_ += ",";
            url_ += longitude;
            url_ += "&sensor=false&language=";
            url_ += Locale.getDefault().getLanguage();
        }

        @Override
        void process(JSONObject result, HttpTask task) {
            try {
                JSONArray arr = getArray(result, "results");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject addr = arr.getJSONObject(i);
                    JSONArray types = getArray(addr, "types");
                    for (int n = 0; n < types.length(); n++) {
                        if (types.getString(n).equals("street_address")) {
                            address_ = addr.getString("formatted_address");
                            return;
                        }
                    }
                }
                address_ = getString(result, "results[0].formatted_address");
            } catch (Exception e) {
                // ignore
            }
        }

        @Override
        boolean postProcess(SharedPreferences.Editor ed) {
            if (address_ != null)
                ed.putString(Names.ADDRESS, address_);
            return true;
        }

        String address_;

    }

    class TemperatureRequest extends HttpRequest {

        String temperature;

        TemperatureRequest(double time, String api_key) {
            url_ = "http://api.car-online.ru/v2?get=temperaturelist&skey=";
            url_ += api_key;
            url_ += "&begin=" + ((long) time - 12 * 60 * 60 * 1000);
            url_ += "&end=" + (long) time;
            url_ += "&content=json";
        }

        @Override
        void process(JSONObject result, HttpTask task) {
            temperature = getString(result, "temperatureList[0].value");
        }

        @Override
        boolean postProcess(SharedPreferences.Editor ed) {
            if (temperature != null)
                ed.putString(Names.TEMPERATURE, temperature);
            return true;
        }

    }

    class LastInfoRequest extends HttpRequest {

        long last_time;
        long last_id;
        long prev_id;

        String main_voltage;
        String reserve_voltage;

        int guard;

        int door;
        int trunk;
        int ignition;
        int hood;

        int door_alarm;
        int trunk_alarm;
        int ignition_alarm;
        int hood_alarm;
        int accessory_alarm;

        String balance_;
        String api_key_;

        String latitude_;
        String longitude_;
        String speed_;

        String latitude_prev;
        String longitude_prev;

        LastInfoRequest(SharedPreferences preferences, String api_key) {
            url_ = "http://api.car-online.ru/v2?get=lastinfo&skey=";
            url_ += api_key;
            url_ += "&content=json";
            api_key_ = api_key;
            latitude_prev = preferences.getString(Names.LATITUDE, "");
            longitude_prev = preferences.getString(Names.LONGITUDE, "");
            prev_id = preferences.getLong(Names.EVENT_ID, 0);
        }

        @Override
        void process(JSONObject result, HttpTask task) {
            last_id = getLong(result, "event.eventId");
            if (last_id == prev_id)
                return;
            if (last_id == 0)
                return;
            last_time = getLong(result, "event.eventTime");
            main_voltage = getString(result, "voltage.main");
            reserve_voltage = getString(result, "voltage.reserved");
            balance_ = getString(result, "balance.source");
            if (last_time != 0)
                task.add(new TemperatureRequest(last_time, api_key_));

            longitude_ = getString(result, "gps.longitude");
            latitude_ = getString(result, "gps.latitude");
            speed_ = getString(result, "gps.speed");
            if ((longitude_ != null) || (latitude_ != null)) {
                if (!longitude_.equals(longitude_prev) || !latitude_.equals(latitude_prev))
                    task.add(new LocationRequest(longitude_, latitude_));
            }

            guard = getBoolean(result, "contact.stGuard");

            door = getBoolean(result, "contact.stInput1");
            trunk = getBoolean(result, "contact.stInput2");
            ignition = getBoolean(result, "contact.stInput3");
            hood = getBoolean(result, "contact.stInput4");

            door_alarm = getBoolean(result, "contact.stZoneDoor");
            hood_alarm = getBoolean(result, "contact.stZoneHood");
            trunk_alarm = getBoolean(result, "contact.stZoneTrunk");
            accessory_alarm = getBoolean(result, "contact.stZoneAccessoryOn");
            ignition_alarm = getBoolean(result, "contact.stZoneIgnitionOn");
        }

        @Override
        boolean postProcess(SharedPreferences.Editor ed) {
            if (last_id == prev_id)
                return false;
            if (last_id == 0)
                return false;
            ed.putLong(Names.EVENT_ID, last_id);
            if (last_time != 0)
                ed.putLong(Names.LAST_EVENT, last_time);
            if (main_voltage != null)
                ed.putString(Names.VOLTAGE, main_voltage);
            if (reserve_voltage != null)
                ed.putString(Names.RESERVE, reserve_voltage);
            if (longitude_ != null)
                ed.putString(Names.LONGITUDE, longitude_);
            if (latitude_ != null)
                ed.putString(Names.LATITUDE, latitude_);
            if (speed_ != null)
                ed.putString(Names.SPEED, speed_);
            if (balance_ != null) {
                Matcher m = balancePattern.matcher(balance_);
                if (m.find())
                    ed.putString(Names.BALANCE, m.group(0));
            }
            ed.putInt(Names.GUARD, guard);
            ed.putInt(Names.DOOR, door);
            ed.putInt(Names.HOOD, hood);
            ed.putInt(Names.TRUNK, trunk);
            ed.putInt(Names.IGNITION, ignition);
            ed.putInt(Names.DOOR_ALARM, door_alarm);
            ed.putInt(Names.HOOD_ALARM, hood_alarm);
            ed.putInt(Names.TRUNK_ALARM, trunk_alarm);
            ed.putInt(Names.IGNITION_ALARM, ignition_alarm);
            ed.putInt(Names.ACCESSORY_ALARM, accessory_alarm);
            return true;
        }
    }

    void startRequest() {
        if (process_request) {
            State.appendLog("Already processed");
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String api_key = preferences.getString(Names.KEY, "");
        if (api_key.length() == 0)
            return;
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if ((activeNetwork == null) || !activeNetwork.isConnected()) {
            State.appendLog("No connection wait...");
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(mReceiver, filter);
            return;
        }
        if (!powerMgr.isScreenOn()) {
            State.appendLog("Screen is off wait");
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            registerReceiver(mReceiver, filter);
            return;
        }
        process_request = true;
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // ignore
        }
        alarmMgr.cancel(pi);
        new HttpTask(new LastInfoRequest(preferences, api_key));
    }

    static final Pattern keyPattern = Pattern.compile("^(.*)\\[([0-9]+)\\]$");

    static JSONObject get(JSONObject obj, String key) throws JSONException {
        Matcher m = keyPattern.matcher(key);
        if (m.find()) {
            String k = m.group(1);
            JSONArray arr = obj.getJSONArray(k);
            return arr.getJSONObject(Integer.parseInt(m.group(2)));
        }
        return obj.getJSONObject(key);
    }

    static JSONArray getArray(JSONObject obj, String key) {
        try {
            String[] sub_keys = key.split("\\.");
            for (int i = 0; i < sub_keys.length - 1; i++) {
                obj = get(obj, sub_keys[i]);
            }
            return obj.getJSONArray(sub_keys[sub_keys.length - 1]);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    static long getLong(JSONObject obj, String key) {
        try {
            String[] sub_keys = key.split("\\.");
            for (int i = 0; i < sub_keys.length - 1; i++) {
                obj = get(obj, sub_keys[i]);
            }
            return obj.getLong(sub_keys[sub_keys.length - 1]);
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    static String getString(JSONObject obj, String key) {
        try {
            String[] sub_keys = key.split("\\.");
            for (int i = 0; i < sub_keys.length - 1; i++) {
                obj = get(obj, sub_keys[i]);
            }
            return obj.getString(sub_keys[sub_keys.length - 1]);
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    static int getBoolean(JSONObject obj, String key) {
        try {
            String[] sub_keys = key.split("\\.");
            for (int i = 0; i < sub_keys.length - 1; i++) {
                obj = get(obj, sub_keys[i]);
            }
            return obj.getBoolean(sub_keys[sub_keys.length - 1]) ? 1 : -1;
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

}
