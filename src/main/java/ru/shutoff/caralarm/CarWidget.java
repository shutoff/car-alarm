package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CarWidget extends AppWidgetProvider {

    static final int UPDATE_INTERVAL = 5 * 1000;
    final String UPDATE_ALL_WIDGETS = "update_all_widgets";

    CarDrawable drawable;
    AlarmManager alarmMgr;
    PendingIntent pi;
    BroadcastReceiver br;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (drawable == null)
            drawable = new CarDrawable(context);
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, i);
        }
    }

    @Override
    public void onEnabled (Context context) {
        super.onEnabled(context);

        br = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWidgets(context);
                stopTimer();
                startTimer(false);
            }
        };
        IntentFilter intFilter = new IntentFilter(StatusService.ACTION_UPDATE);
        context.registerReceiver(br, intFilter);

        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CarWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        startTimer(true);
    }

    @Override
    public void onDisabled (Context context){
        super.onDisabled(context);
        context.unregisterReceiver(br);
        stopTimer();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(UPDATE_ALL_WIDGETS)){
            Intent si = new Intent(context, StatusService.class);
            context.startService(si);
            return;
        }
        super.onReceive(context, intent);
    }

    void startTimer(boolean now) {
        alarmMgr.setInexactRepeating(AlarmManager.RTC, now ? UPDATE_INTERVAL : 0, UPDATE_INTERVAL, pi);
    }

    void stopTimer() {
        alarmMgr.cancel(pi);
    }

    void updateWidgets(Context context){
        ComponentName thisAppWidget = new ComponentName(
                context.getPackageName(), getClass().getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null){
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID : ids) {
                updateWidget(context, appWidgetManager, appWidgetID);
            }
        }
    }

    void updateWidget(Context ctx, AppWidgetManager appWidgetManager, int widgetID) {
        RemoteViews widgetView = new RemoteViews(ctx.getPackageName(), R.layout.widget);

        Intent configIntent = new Intent(ctx, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(ctx, widgetID, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.widget, pIntent);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        long last = preferences.getLong(Names.LAST_EVENT, 0);
        if (last != 0){
            Date d = new Date(last);
            SimpleDateFormat sf = new SimpleDateFormat();
            widgetView.setTextViewText(R.id.last, sf.format(d));
        }else{
            widgetView.setTextViewText(R.id.last, ctx.getString(R.string.unknown));
        }

        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VOLTAGE, "?") + " V");
        widgetView.setTextViewText(R.id.balance, preferences.getString(Names.BALANCE, "?"));
        widgetView.setTextViewText(R.id.temperature, preferences.getString(Names.TEMPERATURE, "?") + " °C");

        if (drawable.update(preferences))
            widgetView.setImageViewBitmap(R.id.car, drawable.getBitmap());

        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

}
