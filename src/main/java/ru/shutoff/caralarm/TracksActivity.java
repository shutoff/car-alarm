package ru.shutoff.caralarm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

public class TracksActivity extends ActionBarActivity {

    SharedPreferences preferences;
    String api_key;

    final static String TELEMETRY = "http://api.car-online.ru/v2?get=telemetry&skey=$1&begin=$2&end=$3&content=json";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracks);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        api_key = preferences.getString(Names.KEY, "");

        LocalTime time = new LocalTime(0, 0);
        LocalDateTime start = time.toDateTimeToday().toLocalDateTime();
        LocalDateTime finish = start.plusDays(1);
        HttpTask telemetry = new HttpTask(){

            @Override
            void result(JSONObject data) throws JSONException {
                int ways = data.getInt("wayscount");
                if (ways > 0){
                    long engine_time = data.getLong("engineTime");
                    double maxSpeed = data.getDouble("maxSpped");
                    double avgSpeed = data.getDouble("averageSpeed");
                    double distance = data.getDouble("milleage");
                }
            }
        };

        telemetry.execute(TELEMETRY,
                api_key,
                start.toDateTime().toDate().getTime() + "",
                finish.toDateTime().toDate().getTime() + "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
