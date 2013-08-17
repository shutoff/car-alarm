package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends ActionBarActivity {

    ImageView imgCar;
    TextView tvAddress;
    TextView tvLast;
    TextView tvVoltage;
    TextView tvReserve;
    TextView tvBalance;
    TextView tvTemperature;

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 1 * 60 * 1000;

    PendingIntent pi;

    BroadcastReceiver br;
    AlarmManager alarmMgr;

    SharedPreferences preferences;
    CarDrawable drawable;
    Address address;

    boolean active;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        State.setExceptionHandler();

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        setContentView(R.layout.main);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        imgCar = (ImageView) findViewById(R.id.car);
        tvAddress = (TextView) findViewById(R.id.address);
        tvLast = (TextView) findViewById(R.id.last);
        tvVoltage = (TextView) findViewById(R.id.voltage);
        tvReserve = (TextView) findViewById(R.id.reserve);
        tvBalance = (TextView) findViewById(R.id.balance);
        tvTemperature = (TextView) findViewById(R.id.temperature);

        drawable = new CarDrawable(this);
        imgCar.setImageDrawable(drawable.getDrawable());

        removeNotifications();
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
                stopTimer();
                startTimer(false);
            }
        };
        IntentFilter intFilter = new IntentFilter(StatusService.ACTION_UPDATE);
        registerReceiver(br, intFilter);

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);


        String phone = preferences.getString(Names.PHONE, "");
        String key = preferences.getString(Names.KEY, "");
        if ((phone.length() == 0) || (key.length() == 0)) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }

        tvAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), MapView.class);
                startActivity(intent);
            }
        });

        active = false;

        address = new Address(preferences) {
            @Override
            void onResult() {
                tvAddress.setText(
                    preferences.getString(Names.Latitude, "") + " " +
                    preferences.getString(Names.Longitude, "") + "\n" +
                    preferences.getString(Names.Address, ""));
            }
        };
        address.update();
        address.onResult();
        update();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        startTimer(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
        stopTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ALARM) {
            Intent intent = new Intent(this, StatusService.class);
            startService(intent);
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
    protected void onNewIntent(Intent intent) {
        removeNotifications();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void removeNotifications() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int id = preferences.getInt(Names.IDS, 0);
        for (int i = 1; i <= id; i++) {
            try {
                manager.cancel(i);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.IDS, 0);
        ed.commit();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences: {
                Intent intent = new Intent(this, Preferences.class);
                startActivity(intent);
                break;
            }
            case R.id.actions: {
                Intent intent = new Intent(this, Actions.class);
                startActivity(intent);
                break;
            }
            case R.id.map: {
                Intent intent = new Intent(this, MapView.class);
                startActivity(intent);
                break;
            }
            case R.id.tracks: {
                Intent intent = new Intent(this, TracksActivity.class);
                startActivity(intent);
                break;
            }
        }
        return false;
    }

    void update() {
        long last = preferences.getLong(Names.EventTime, 0);
        if (last != 0) {
            Date d = new Date(last);
            SimpleDateFormat sf = new SimpleDateFormat();
            tvLast.setText(sf.format(d));
        } else {
            tvLast.setText(getString(R.string.unknown));
        }
        tvVoltage.setText(preferences.getString(Names.VoltageMain, "?") + " V");
        tvReserve.setText(preferences.getString(Names.VoltageReserved, "?") + " V");
        tvBalance.setText(preferences.getString(Names.Balance, "?"));
        tvTemperature.setText(preferences.getString(Names.Temperature, "?") + " °C");

        drawable.update(preferences);
        address.update();
    }

}
