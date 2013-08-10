package ru.shutoff.caralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CarWidget extends AppWidgetProvider {

    static final int UPDATE_INTERVAL = 5 * 60 * 1000;
    static final String UPDATE_ALL_WIDGETS = "update_all_widgets";

    CarDrawable drawable;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, i);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Intent intent = new Intent(context, CarWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                UPDATE_INTERVAL, pIntent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent intent = new Intent(context, CarWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(StatusService.ACTION_UPDATE)) {
            updateWidgets(context);
            PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pIntent);
            PowerManager powerMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerMgr.isScreenOn())
                alarmManager.setInexactRepeating(AlarmManager.RTC,
                        System.currentTimeMillis() + UPDATE_INTERVAL, UPDATE_INTERVAL, pIntent);
            return;
        }
        if (intent.getAction().equalsIgnoreCase(UPDATE_ALL_WIDGETS)) {
            Intent si = new Intent(context, StatusService.class);
            context.startService(si);
            return;
        }
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {
            Intent si = new Intent(context, StatusService.class);
            context.startService(si);
            return;
        }
        super.onReceive(context, intent);
    }

    void updateWidgets(Context context) {
        ComponentName thisAppWidget = new ComponentName(
                context.getPackageName(), getClass().getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID : ids) {
                updateWidget(context, appWidgetManager, appWidgetID);
            }
        }
    }

    void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

        Intent configIntent = new Intent(context, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, widgetID, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.widget, pIntent);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long last = preferences.getLong(Names.LAST_EVENT, 0);
        if (last != 0) {
            Date d = new Date(last);
            SimpleDateFormat sf = new SimpleDateFormat();
            widgetView.setTextViewText(R.id.last, sf.format(d));
        } else {
            widgetView.setTextViewText(R.id.last, context.getString(R.string.unknown));
        }

        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VOLTAGE, "?") + " V");
        widgetView.setTextViewText(R.id.balance, preferences.getString(Names.BALANCE, "?"));
        widgetView.setTextViewText(R.id.temperature, preferences.getString(Names.TEMPERATURE, "?") + " °C");

        if (drawable == null)
            drawable = new CarDrawable(context);

        if (drawable.update(preferences))
            widgetView.setImageViewBitmap(R.id.car, drawable.getBitmap());

        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

}
