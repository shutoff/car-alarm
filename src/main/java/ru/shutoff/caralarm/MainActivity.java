package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

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

    Drawable dCar;
/*
    Drawable dDoors;
    Drawable dTrunk;
    Drawable dHood;
    Drawable dLock;
*/

    static final int UNKNOWN = 0x888888;
    static final int NORMAL  = 0xCCCCCC;
    static final int GUARD   = 0x66FF99;
    static final int ALARM   = 0xFF0033;

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 5 * 60 * 1000;

    PendingIntent pi;

    BroadcastReceiver br;
    AlarmManager alarmMgr;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        imgCar = (ImageView) findViewById(R.id.car);
        tvAddress = (TextView)findViewById(R.id.address);
        tvLast = (TextView)findViewById(R.id.last);
        tvVoltage = (TextView)findViewById(R.id.voltage);
        tvReserve = (TextView)findViewById(R.id.reserve);
        tvBalance = (TextView)findViewById(R.id.balance);
        tvTemperature = (TextView)findViewById(R.id.temperature);

        dCar = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.car));
        dCar.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

/*
        dDoors = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.doors));
        dDoors.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dTrunk = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.trunk));
        dTrunk.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dHood = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.hood));
        dHood.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dLock = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.lock));
        dLock.setColorFilter(new ColorMatrixColorFilter(createMatrix(0x000000)));
        dLock.setAlpha(0);

        Drawable[] drawables =
                {
                        dCar,
                        dDoors,
                        dTrunk,
                        dHood,
                        dLock
                };

        LayerDrawable drawable = new LayerDrawable(drawables);
*/

        imgCar.setImageDrawable(dCar);

        removeNotifications();
        br = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        IntentFilter intFilt = new IntentFilter(StatusService.ACTION_UPDATE);
        registerReceiver(br, intFilt);

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String phone = preferences.getString(Names.PHONE, "");
        String key = preferences.getString(Names.KEY, "");
        if ((phone.length() == 0) || (key.length() == 0)){
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }

        update();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(br);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        alarmMgr.setInexactRepeating(AlarmManager.RTC, 0, UPDATE_INTERVAL, pi);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        alarmMgr.cancel(pi);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ALARM){
            Intent intent = new Intent(this, StatusService.class);
            startService(intent);
        }
    }

    static ColorMatrix createMatrix(int color)
    {
        int red   = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue  = color & 0xFF;
        float matrix[] =
                {
                        red / 255f, 0, 0, 0, 0,
                        0, green / 255f, 0, 0, 0,
                        0, 0, blue / 255f, 0, 0,
                        1, 0, 0, 0, 0
                };
        ColorMatrix cm = new ColorMatrix(matrix);
        return cm;
    }

    protected static float cleanValue(float p_val, float p_limit)
    {
        return Math.min(p_limit, Math.max(-p_limit, p_val));
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        removeNotifications();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void removeNotifications() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int id = preferences.getInt(Names.IDS, 0);
        for (int i = 1; i <= id; i++){
            try {
                manager.cancel(i);
            }catch (NumberFormatException e) {
            }
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.IDS, 0);
        ed.commit();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.preferences:
            {
                Intent intent = new Intent(this, Preferences.class);
                startActivity(intent);
                break;
            }
            case R.id.actions:
            {
                Intent intent = new Intent(this, Actions.class);
                startActivity(intent);
            }
        }
        return false;
    }

    void update()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        tvAddress.setText(
                preferences.getString(Names.LATITUDE, "") + " " + preferences.getString(Names.LONGITUDE, "") + "\n" +
                        preferences.getString(Names.ADDRESS, ""));
        long last = preferences.getLong(Names.LAST_EVENT, 0);
        if (last != 0){
            Date d = new Date(last);
            SimpleDateFormat sf = new SimpleDateFormat();
            tvLast.setText(sf.format(d));
        }else{
            tvLast.setText(getString(R.string.unknown));
        }
        tvVoltage.setText(preferences.getString(Names.VOLTAGE, "?") + " V");
        tvReserve.setText(preferences.getString(Names.RESERVE, "?") + " V");
        tvBalance.setText(preferences.getString(Names.BALANCE, "?"));
        tvTemperature.setText(preferences.getString(Names.TEMPERATURE, "?") + " °C");
    }
}
