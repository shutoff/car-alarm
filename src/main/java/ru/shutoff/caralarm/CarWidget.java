package ru.shutoff.caralarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import org.joda.time.LocalDateTime;

import java.util.Date;

public class CarWidget extends AppWidgetProvider {

    CarDrawable drawable;

    static final int STATE_NONE = 0;
    static final int STATE_UPDATE = 1;
    static final int STATE_ERROR = 2;

    static int state = STATE_NONE;

    static final String HEIGHT = "Height_";

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
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        for (int id : appWidgetIds) {
            ed.remove(HEIGHT + id);
        }
        ed.commit();
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
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equalsIgnoreCase(StatusService.ACTION_UPDATE)) {
                    state = STATE_NONE;
                    updateWidgets(context);
                    Intent i = new Intent(context, WidgetService.class);
                    i.setAction(WidgetService.ACTION_UPDATE);
                    context.startService(i);
                }
                if (action.equalsIgnoreCase(StatusService.ACTION_NOUPDATE)) {
                    if (state != STATE_NONE) {
                        state = STATE_NONE;
                        updateWidgets(context);
                    }
                }
                if (action.equalsIgnoreCase(StatusService.ACTION_ERROR)) {
                    if (state != STATE_ERROR) {
                        state = STATE_ERROR;
                        updateWidgets(context);
                    }
                }
                if (action.equalsIgnoreCase(StatusService.ACTION_START)) {
                    if (state != STATE_UPDATE) {
                        state = STATE_UPDATE;
                        updateWidgets(context);
                    }
                }
                if (action.equalsIgnoreCase(StatusService.ACTION_START)) {
                    if (state != STATE_UPDATE) {
                        state = STATE_UPDATE;
                        updateWidgets(context);
                        Intent i = new Intent(context, StatusService.class);
                        context.startService(i);
                    }
                }
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(HEIGHT + appWidgetId, minHeight);
        ed.commit();
        updateWidget(context, appWidgetManager, appWidgetId);
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

        configIntent = new Intent(StatusService.ACTION_START_UPDATE);
        pIntent = PendingIntent.getBroadcast(context, 0, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.update_block, pIntent);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long last = preferences.getLong(Names.EventTime, 0);
        Date now = new Date();
        if (last > now.getTime() - 24 * 60 * 60 * 1000) {
            LocalDateTime d = new LocalDateTime(last);
            widgetView.setTextViewText(R.id.last, d.toString("HH:mm"));
        } else {
            widgetView.setTextViewText(R.id.last, "??:??");
        }

        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VoltageMain, "--") + " V");
        widgetView.setTextViewText(R.id.reserve, preferences.getString(Names.VoltageReserved, "--") + " V");
        widgetView.setTextViewText(R.id.balance, preferences.getString(Names.Balance, "---.--"));
        widgetView.setTextViewText(R.id.temperature, preferences.getString(Names.Temperature, "--") + " \u00B0C");

        int height = preferences.getInt(HEIGHT + widgetID, 40);
        widgetView.setViewVisibility(R.id.reserve_block, (height < 60) ? View.GONE : View.VISIBLE);

        switch (state) {
            case STATE_UPDATE:
                widgetView.setViewVisibility(R.id.refresh, View.GONE);
                widgetView.setViewVisibility(R.id.update, View.VISIBLE);
                widgetView.setViewVisibility(R.id.error, View.GONE);
                break;
            case STATE_ERROR:
                widgetView.setViewVisibility(R.id.refresh, View.GONE);
                widgetView.setViewVisibility(R.id.update, View.GONE);
                widgetView.setViewVisibility(R.id.error, View.VISIBLE);
                break;
            default:
                widgetView.setViewVisibility(R.id.refresh, View.VISIBLE);
                widgetView.setViewVisibility(R.id.update, View.GONE);
                widgetView.setViewVisibility(R.id.error, View.GONE);
        }

        if (drawable == null)
            drawable = new CarDrawable(context);

        drawable.update(preferences);
        widgetView.setImageViewBitmap(R.id.car, drawable.getBitmap());

        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

}
