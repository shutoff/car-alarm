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

    static final int UPDATE_INTERVAL   = 5 * 60 * 1000;
    static final int INACTIVE_INTERVAL = 60 * 60 * 1000;

    static final String ACTION_STOP = "ru.shutoff.caralarm.WIDGET_STOP";
    static final String ACTION_UPDATE = "ru.shutoff.caralarm.WIDGET_UPDATE";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    PowerManager powerMgr;
    AlarmManager alarmMgr;
    PendingIntent pi;
    BroadcastReceiver br;

    @Override
    public void onCreate() {
        super.onCreate();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.appendLog("Error thread: " + thread.toString());
                State.appendLog("Error: " + ex.toString());
            }
        });

        State.appendLog("WidgetService.onStart");
        powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getService(this, 0, new Intent(this, WidgetService.class), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {
                    State.appendLog("Widget service SCREEN ON - Update now");
                    stopTimer();
                    startTimer(0, UPDATE_INTERVAL);
                }
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
                    State.appendLog("Widget service SCREEN OFF");
                    stopTimer();
                    startTimer(INACTIVE_INTERVAL, INACTIVE_INTERVAL);
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(br, filter);
    }

    @Override
    public void onDestroy() {
        State.appendLog("WidgetService.onDestroy");
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_STOP)) {
                State.appendLog("Widget service disabled");
                stopTimer();
                stopSelf();
                return START_STICKY;
            }
            if (action.equals(ACTION_UPDATE)) {
                State.appendLog("Widget service update");
                stopTimer();
                if (powerMgr.isScreenOn()){
                    startTimer(UPDATE_INTERVAL, UPDATE_INTERVAL);
                }else{
                    startTimer(INACTIVE_INTERVAL, INACTIVE_INTERVAL);
                }
                return START_STICKY;
            }
        }
        State.appendLog("Widget service do update");
        Intent i = new Intent(this, StatusService.class);
        startService(i);
        return START_STICKY;
    }

    void startTimer(long first, long interval) {
        State.appendLog("Widget service start timer " + first + "," + interval);
        alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + first, interval, pi);
    }

    void stopTimer() {
        State.appendLog("Widget service stop timer");
        alarmMgr.cancel(pi);
    }
}
