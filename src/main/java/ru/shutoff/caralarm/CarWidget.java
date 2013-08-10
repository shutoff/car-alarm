package ru.shutoff.caralarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CarWidget extends AppWidgetProvider {

//    static final int WIDGET_UPDATE_TIMEOUT = 5 * 60 * 1000;

    CarDrawable drawable;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (drawable == null)
            drawable = new CarDrawable(context);
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, i);
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
