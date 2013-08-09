package ru.shutoff.caralarm;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class StatusService extends Service {

	boolean	process_request;
	
	final long REPEAT_AFTER_ERROR = 20 * 1000;
	
    BroadcastReceiver mReceiver;
    PendingIntent    pi;
    
	ConnectivityManager conMgr;
	PowerManager powerMgr;
	AlarmManager alarmMgr;
	
	static final String  ACTION_UPDATE = "ru.shutoff.caralarm.update";
	
	static final Pattern balancePattern = Pattern.compile("^-?[0-9]+\\.[0-9][0-9]");
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate () {
		super.onCreate();
		process_request = false;
		conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
		alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		pi = PendingIntent.getService(this, 0, new Intent(this, StatusService.class), 0);
        mReceiver = new ScreenReceiver();
	}
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
		startRequest();
	    return START_STICKY;
	}
	
	abstract class HttpRequest {
		
		abstract void process(JSONObject result, HttpTask task);
		abstract void postProcess(SharedPreferences.Editor ed);
		String	url_;
	};
	
	class HttpTask extends AsyncTask<Void, Void, Void> {

		List<HttpRequest> requests_;
		String			  error_;
		
		HttpTask(HttpRequest request)
		{
			requests_ = new Vector<HttpRequest>();
			requests_.add(request);
			execute();
		}
		
		void add(HttpRequest request)
		{
			requests_.add(request);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
		    HttpClient httpclient = new DefaultHttpClient();
			for (int i = 0; i < requests_.size(); i++){
				try{
					HttpRequest req = requests_.get(i);
					String url = req.url_;
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
				} catch (Exception e){
					error_ = e.getMessage();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		    SharedPreferences.Editor ed = preferences.edit();
			for (int i = 0; i < requests_.size(); i++){
				requests_.get(i).postProcess(ed);
			}
			ed.commit();
			process_request = false;
			sendUpdate();
			if (error_ != null)
				alarmMgr.setInexactRepeating(AlarmManager.RTC, REPEAT_AFTER_ERROR, REPEAT_AFTER_ERROR, pi);
		}	
	}
	
	void sendUpdate() {
		try {
			Intent intent = new Intent(ACTION_UPDATE);
			sendBroadcast(intent);
		} catch (Exception e){
		}
	}
	
	class LocationRequest extends HttpRequest {

		LocationRequest(String longitude, String latitude)
		{
			url_ = "http://maps.googleapis.com/maps/api/geocode/json?latlng=";
			url_ += latitude;
			url_ += ",";
			url_ += longitude;
			url_ += "&sensor=true&language=";
			url_ += Locale.getDefault().getLanguage();
		}
		
		@Override
		void process(JSONObject result, HttpTask task) {
			address_ = getString(result, "results[0].formatted_address");
		}

		@Override
		void postProcess(SharedPreferences.Editor ed) {
			if (address_ != null)
				ed.putString(Names.ADDRESS, address_);
		}
		
		String address_;
		
	};
	
	class TemperatureRequest extends HttpRequest {

		String temperature;
		
		TemperatureRequest(double time, String api_key) {
		    url_ = "http://api.car-online.ru/v2?get=temperaturelist&skey=";
		    url_ += api_key;
		    url_ += "&begin=" + ((long)time - 12 * 60 * 60 * 1000);
		    url_ += "&end=" + (long)time;
		    url_ += "&content=json";
		}

		@Override
		void process(JSONObject result, HttpTask task) {
			temperature = getString(result, "temperatureList[0].value");
		}

		@Override
		void postProcess(SharedPreferences.Editor ed) {
			if (temperature != null)
				ed.putString(Names.TEMPERATURE, temperature);
		}
		
	};
	
	class LastInfoRequest extends HttpRequest {

		double last_time;
		
		String main_voltage;
		String reserve_voltage;
		
		boolean guard;
		boolean door;
		boolean hood;
		boolean trunk;
		
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
		    latitude_prev  = preferences.getString(Names.LATITUDE, "");
		    longitude_prev = preferences.getString(Names.LONGITUDE, ""); 
		}

		@Override
		void process(JSONObject result, HttpTask task) {
			last_time = getDouble(result, "gps.eventTime");
			main_voltage = getString(result, "voltage.main");
			reserve_voltage = getString(result, "voltage.reserved");
			balance_ = getString(result, "balance.source");
			if (last_time != 0)
				task.add(new TemperatureRequest(last_time, api_key_));
	
			longitude_ = getString(result, "gps.longitude");
			latitude_  = getString(result, "gps.latitude");
			speed_     = getString(result, "gps.speed");
			if ((longitude_ != null) || (latitude_ != null)){
				if (!longitude_.equals(longitude_prev) || !latitude_.equals(latitude_prev))
					task.add(new LocationRequest(longitude_, latitude_));
			}
			
			guard = getBoolean(result, "contact.stGuard");
			door  = getBoolean(result, "contact.stZoneDoor");
			hood  = getBoolean(result, "contact.stZoneHood");
			trunk = getBoolean(result, "contact.stZoneTrunk");
		}

		@Override
		void postProcess(SharedPreferences.Editor ed) {
			if (last_time != 0)
				ed.putLong(Names.LAST_EVENT, (long)last_time);
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
			if (balance_ != null){
				Matcher m = balancePattern.matcher(balance_);
				if (m.find())
					ed.putString(Names.BALANCE, m.group(0));
			}
			ed.putBoolean(Names.GUARD, guard);
			ed.putBoolean(Names.DOOR, door);
			ed.putBoolean(Names.HOOD, hood);
			ed.putBoolean(Names.TRUNK, trunk);
		}
	};

	void startRequest(){
		if (process_request)
			return;
	    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    String api_key = preferences.getString(Names.KEY, "");
	    if (api_key.length() == 0)
	    	return;
		process_request = true;
		final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		if ((activeNetwork == null) || !activeNetwork.isConnected()){
			IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(mReceiver, filter);
			return;	
		}
		if (!powerMgr.isScreenOn()){
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            registerReceiver(mReceiver, filter);
			return;
		}
		try{
			unregisterReceiver(mReceiver);
		} catch (Exception e){
		}
		alarmMgr.cancel(pi);
	    new HttpTask(new LastInfoRequest(preferences, api_key));
	}
	
	static final Pattern keyPattern = Pattern.compile("^(.*)\\[([0-9]+)\\]$");
	
	static JSONObject get(JSONObject obj, String key) throws JSONException
	{
		Matcher m = keyPattern.matcher(key);
		if (m.find()){
			String k = m.group(1);
			JSONArray arr = obj.getJSONArray(k);
			return arr.getJSONObject(Integer.parseInt(m.group(2)));
		}
		return obj.getJSONObject(key);
	}
	
	static double getDouble(JSONObject obj, String key)
	{
		try{
			String[] sub_keys = key.split("\\.");
			for (int i = 0; i < sub_keys.length - 1; i++){
				obj = get(obj, sub_keys[i]);
			}
			return obj.getDouble(sub_keys[sub_keys.length - 1]);
		}catch (Exception e){			
		}
		return 0;
	}
	
	static String getString(JSONObject obj, String key)
	{
		try{
			String[] sub_keys = key.split("\\.");
			for (int i = 0; i < sub_keys.length - 1; i++){
				obj = get(obj, sub_keys[i]);
			}
			return obj.getString(sub_keys[sub_keys.length - 1]);
		}catch (Exception e){
		}
		return "";
	}
	
	static boolean getBoolean(JSONObject obj, String key)
	{
		try{
			String[] sub_keys = key.split("\\.");
			for (int i = 0; i < sub_keys.length - 1; i++){
				obj = get(obj, sub_keys[i]);
			}
			return obj.getBoolean(sub_keys[sub_keys.length - 1]);
		}catch (Exception e){
		}
		return false;
	}

}
