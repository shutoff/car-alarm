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
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, i);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
        updateWidgets(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent i = new Intent(context, WidgetService.class);
        i.setAction(WidgetService.ACTION_STOP);
        context.startService(i);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ((action != null) && action.equalsIgnoreCase(StatusService.ACTION_UPDATE)) {
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
        long last = preferences.getLong(Names.EventTime, 0);
        if (last != 0) {
            Date d = new Date(last);
            SimpleDateFormat sf = new SimpleDateFormat();
            widgetView.setTextViewText(R.id.last, sf.format(d));
        } else {
            widgetView.setTextViewText(R.id.last, context.getString(R.string.unknown));
        }

        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VoltageMain, "--") + " V");
        widgetView.setTextViewText(R.id.balance, preferences.getString(Names.Balance, "---.--"));
        widgetView.setTextViewText(R.id.temperature, preferences.getString(Names.Temperature, "--") + " \u00B0C");

        if (drawable == null)
            drawable = new CarDrawable(context);

        drawable.update(preferences);
        widgetView.setImageViewBitmap(R.id.car, drawable.getBitmap());

        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

}
