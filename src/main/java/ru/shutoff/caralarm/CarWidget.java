package ru.shutoff.caralarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CarWidget extends AppWidgetProvider {

    CarDrawable drawable;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        State.appendLog("Widget onUpdate");
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, i, true);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        State.appendLog("Widget onEnabled");
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
        updateWidgets(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        State.appendLog("Widget onDisabled");
        Intent i = new Intent(context, WidgetService.class);
        i.setAction(WidgetService.ACTION_STOP);
        context.startService(i);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(StatusService.ACTION_UPDATE)) {
            State.appendLog("Update widgets");
            updateWidgets(context);
            Intent i = new Intent(context, WidgetService.class);
            i.setAction(WidgetService.ACTION_UPDATE);
            context.startService(i);
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
                updateWidget(context, appWidgetManager, appWidgetID, false);
            }
        }
    }

    void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID, boolean force) {
        State.appendLog("Update widget " + widgetID);
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

        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VOLTAGE, "--") + " V");
        widgetView.setTextViewText(R.id.balance, preferences.getString(Names.BALANCE, "---.--"));
        widgetView.setTextViewText(R.id.temperature, preferences.getString(Names.TEMPERATURE, "--") + " °C");

        if (drawable == null)
            drawable = new CarDrawable(context);

        if (drawable.update(preferences) || force)
            widgetView.setImageViewBitmap(R.id.car, drawable.getBitmap());

        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

}
