package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;

public class WidgetService extends Service {

    static final int UPDATE_INTERVAL = 5 * 60 * 1000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    PowerManager  powerMgr;
    AlarmManager  alarmMgr;
    PendingIntent pi;
    BroadcastReceiver br;

    @Override
    public void onCreate() {
        super.onCreate();
        powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getService(this, 0, new Intent(this, WidgetService.class), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(StatusService.ACTION_UPDATE)) {
                    State.appendLog("Widgets service update...");
                    stopTimer();
                    if (powerMgr.isScreenOn())
                        startTimer(false);
                }
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {
                    State.appendLog("Widget service SCREEN ON - Update now");
                    stopTimer();
                    startTimer(true);
                }
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
                    State.appendLog("Widget service SCREEN OFF");
                    stopTimer();
                }
            }
        };
        registerReceiver(br, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(br, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(br, new IntentFilter(StatusService.ACTION_UPDATE));

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getIntExtra(Names.STOP, 0) == 1){
            State.appendLog("Widget service disabled");
            stopTimer();
            stopSelf();
            return START_STICKY;
        }
        State.appendLog("Widget service cmd");
        stopTimer();
        startTimer(false);
        Intent i = new Intent(this, StatusService.class);
        startService(i);
        return START_STICKY;
    }

    void startTimer(boolean now) {
        State.appendLog("Widget start timer" +(now ? " now" : ""));
        alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + (now ? 0 : UPDATE_INTERVAL), UPDATE_INTERVAL, pi);
    }

    void stopTimer() {
        State.appendLog("Widget stop timer");
        alarmMgr.cancel(pi);
    }
}
