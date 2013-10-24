package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.joda.time.LocalDateTime;

import java.util.HashMap;
import java.util.Map;

public class MapView extends WebViewActivity {

    SharedPreferences preferences;
    BroadcastReceiver br;
    String car_id;
    String point_data;
    Map<String, String> times;
    AlarmManager alarmMgr;
    PendingIntent pi;
    boolean active;
    LocationManager locationManager;
    Location currentBestLocation;
    LocationListener netListener;
    LocationListener gpsListener;
    Cars.Car[] cars;

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 30 * 1000;

    class JsInterface {

        @JavascriptInterface
        public String getLocation() {
            if (currentBestLocation == null)
                return "";
            String res = currentBestLocation.getLatitude() + ",";
            res += currentBestLocation.getLongitude() + ",";
            res += currentBestLocation.getAccuracy();
            if (currentBestLocation.hasBearing())
                res += currentBestLocation.getBearing();
            State.appendLog("location: " + res);
            return res;
        }

        @JavascriptInterface
        public String getData() {

            Cars.Car[] cars = Cars.getCars(getBaseContext());
            String[] car_data = new String[cars.length];
            for (int i = 0; i < cars.length; i++) {
                String id = cars[i].id;
                String data = id + ";" +
                        preferences.getString(Names.LATITUDE + id, "0") + ";" +
                        preferences.getString(Names.LONGITUDE + id, "0") + ";" +
                        preferences.getString(Names.COURSE + id, "0") + ";";
                if (cars.length > 1) {
                    String name = preferences.getString(Names.CAR_NAME + id, "");
                    if (name.length() == 0) {
                        name = getString(R.string.car);
                        if (id.length() > 0)
                            name += " " + id;
                    }
                    data += name + "<br/>";
                }
                long last_stand = preferences.getLong(Names.LAST_STAND + id, 0);
                if (last_stand > 0) {
                    LocalDateTime stand = new LocalDateTime(last_stand);
                    LocalDateTime now = new LocalDateTime();
                    data += "<b>";
                    if (stand.toLocalDate().equals(now.toLocalDate())) {
                        data += stand.toString("HH:mm");
                    } else {
                        data += stand.toString("d-MM-yy HH:mm");
                    }
                    data += "</b> ";
                } else if (last_stand < 0) {
                    String speed = preferences.getString(Names.SPEED + id, "");
                    try {
                        double s = Double.parseDouble(speed);
                        if (s > 0) {
                            data += String.format(getString(R.string.speed, speed));
                            data += "<br/>";
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                data += preferences.getString(Names.LATITUDE + id, "0") + ","
                        + preferences.getString(Names.LONGITUDE + id, "0") + "<br/>";
                String address = Address.getAddress(getBaseContext(), id);
                String[] parts = address.split(", ");
                if (parts.length >= 3) {
                    address = parts[0] + ", " + parts[1];
                    for (int n = 2; n < parts.length; n++)
                        address += "<br/>" + parts[n];
                }
                data += address;
                car_data[i] = data;
            }

            if (point_data != null) {
                String res = point_data;
                for (String data : car_data) {
                    res += "|" + data;
                }
                State.appendLog("data: " + res);
                return res;
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
            State.appendLog("data: " + first);
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
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        car_id = getIntent().getStringExtra(Names.ID);
        point_data = getIntent().getStringExtra(Names.POINT_DATA);
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

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                webView.loadUrl("javascript:update()");
                stopTimer();
                startTimer(false);
            }
        };
        registerReceiver(br, new IntentFilter(FetchService.ACTION_UPDATE));

        netListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationChanged(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationChanged(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        currentBestLocation = getLastBestLocation();

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, gpsListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, netListener);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
        locationManager.removeUpdates(netListener);
        locationManager.removeUpdates(gpsListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        startTimer(true);
        setActionBar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
        stopTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ALARM) {
            Intent intent = new Intent(this, FetchService.class);
            intent.putExtra(Names.ID, car_id);
            startService(intent);
        }
    }

    void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        cars = Cars.getCars(this);
        if (cars.length > 1) {
            String save_point_data = point_data;
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setListNavigationCallbacks(new CarsAdapter(), new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int i, long l) {
                    if (cars[i].id.equals(car_id))
                        return true;
                    point_data = null;
                    car_id = cars[i].id;
                    webView.loadUrl("javascript:update()");
                    webView.loadUrl("javascript:center()");
                    return true;
                }
            });
            for (int i = 0; i < cars.length; i++) {
                if (cars[i].id.equals(car_id)) {
                    actionBar.setSelectedNavigationItem(i);
                    break;
                }
            }
            point_data = save_point_data;
            setTitle("");
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            setTitle(getString(R.string.app_name));
        }
    }

    void startTimer(boolean now) {
        if (!active)
            return;
        alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + (now ? 0 : UPDATE_INTERVAL), UPDATE_INTERVAL, pi);
    }

    void stopTimer() {
        alarmMgr.cancel(pi);
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
            case R.id.map:
                webView.loadUrl("javascript:center()");
                break;
            case R.id.my:
                webView.loadUrl("javascript:setPosition()");
                break;
        }
        return false;
    }

    Location getLastBestLocation() {
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        long GPSLocationTime = 0;
        if (locationGPS != null)
            GPSLocationTime = locationGPS.getTime();
        long NetLocationTime = 0;
        if (locationNet != null)
            NetLocationTime = locationNet.getTime();
        if (GPSLocationTime > NetLocationTime)
            return locationGPS;
        return locationNet;
    }

    public void locationChanged(Location location) {
        if (isBetterLocation(location, currentBestLocation))
            currentBestLocation = location;
        if (currentBestLocation == null)
            return;
        webView.loadUrl("javascript:myLocation()");
    }

    static final int TWO_MINUTES = 1000 * 60 * 2;

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null)
            return true;

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null)
            return provider2 == null;
        return provider1.equals(provider2);
    }

    class CarsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return cars.length;
        }

        @Override
        public Object getItem(int position) {
            return cars[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_dropdown_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(cars[position].name);
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_dropdown_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(cars[position].name);
            return v;
        }
    }
}
