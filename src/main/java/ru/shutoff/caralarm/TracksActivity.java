package ru.shutoff.caralarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class TracksActivity extends ActionBarActivity {

    SharedPreferences preferences;
    String api_key;

    TextView tvStatus;
    ListView lvTracks;

    CaldroidFragment dialogCaldroidFragment;
    LocalDate current;
    Vector<Track> tracks;

    int days;
    int track_id;
    static final int MAX_DAYS = 7;

    final static String TELEMETRY = "http://api.car-online.ru/v2?get=telemetry&skey=$1&begin=$2&end=$3&content=json";
    final static String WAYSTANDS = "http://api.car-online.ru/v2?get=waystands&skey=$1&begin=$2&end=$3&content=json";
    final static String GPSLIST = "http://api.car-online.ru/v2?get=gpslist&skey=$1&begin=$2&end=$3&content=json";
    static final String ADDRESS_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracks);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        api_key = preferences.getString(Names.KEY, "");
        days = 0;
        track_id = 0;

        tvStatus = (TextView) findViewById(R.id.status);
        lvTracks = (ListView) findViewById(R.id.tracks);

        lvTracks.setClickable(true);
        lvTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TracksAdapter adapter = (TracksAdapter)lvTracks.getAdapter();
                if (adapter.selected == position)
                    return;
                adapter.selected = position;
                adapter.notifyDataSetChanged();
            }
        });

        DataFetcher fetcher = new FirstDataFetcher();
        fetcher.update(new LocalDate());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tracks, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.day: {
                dialogCaldroidFragment = new CaldroidFragment();
                Bundle args = new Bundle();
                args.putString(CaldroidFragment.DIALOG_TITLE, getString(R.string.day));
                args.putInt(CaldroidFragment.MONTH, current.getMonthOfYear());
                args.putInt(CaldroidFragment.YEAR, current.getYear());
                args.putInt(CaldroidFragment.START_DAY_OF_WEEK, 1);
                dialogCaldroidFragment.setArguments(args);
                LocalDate now = new LocalDate();
                dialogCaldroidFragment.setMaxDate(now.toDate());
                CaldroidListener listener = new CaldroidListener() {

                    @Override
                    public void onSelectDate(Date date, View view) {
                        DataFetcher fetcher = new DataFetcher();
                        fetcher.update(new LocalDate(date));
                        dialogCaldroidFragment.dismiss();
                    }
                };
                dialogCaldroidFragment.setCaldroidListener(listener);
                dialogCaldroidFragment.show(getSupportFragmentManager(), "TAG");
                break;
            }
        }
        return false;
    }

    class DataFetcher extends HttpTask {

        LocalDate date;

        boolean noData() {
            return false;
        }

        @Override
        void result(JSONObject data) throws JSONException {
            if (!current.equals(date))
                return;
            tracks = new Vector<Track>();
            int ways = data.getInt("waysCount");
            if (ways == 0) {
                if (noData())
                    return;
                setTitle(date.toString("d MMMM"));
                tvStatus.setText(getString(R.string.no_data));
                return;
            }
            setTitle(date.toString("d MMMM"));
            double mileage = data.getDouble("mileage") / 1000;
            double avg_speed = data.getDouble("averageSpeed");
            double max_speed = data.getDouble("maxSpeed");
            int minutes = data.getInt("engineTime") / 60000;
            String status = getString(R.string.status);
            status = String.format(status, mileage, timeFormat(minutes), avg_speed, max_speed);
            tvStatus.setText(status);
            TracksFetcher tracksFetcher = new TracksFetcher();
            tracksFetcher.update(date);
        }

        void update(LocalDate d) {
            date = d;
            current = d;
            lvTracks.setAdapter(new EmptyTracksAdapter());
            DateTime start = date.toDateTime(new LocalTime(0, 0));
            LocalDate next = date.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            execute(TELEMETRY,
                    api_key,
                    start.toDate().getTime() + "",
                    finish.toDate().getTime() + "");
        }
    }

    class FirstDataFetcher extends DataFetcher {

        boolean noData() {
            if (days > MAX_DAYS) {
                DataFetcher fetcher = new DataFetcher();
                fetcher.update(new LocalDate());
                return true;
            }
            days++;
            LocalDate d = new LocalDate();
            d = d.minusDays(days);
            DataFetcher fetcher = new FirstDataFetcher();
            fetcher.update(d);
            return true;
        }
    }

    class TracksFetcher extends HttpTask {
        int id;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            JSONArray list = res.getJSONArray("waystandlist");
            for (int i = 0; i < list.length(); i++) {
                JSONObject way = list.getJSONObject(i);
                if (!way.getString("type").equals("WAY"))
                    continue;
                JSONArray events = way.getJSONArray("events");
                int last = events.length() - 1;
                Track track = new Track();
                track.begin = events.getJSONObject(0).getLong("eventTime");
                track.end = events.getJSONObject(last).getLong("eventTime");
                tracks.add(track);
            }
            TrackFetcher fetcher = new TrackFetcher();
            fetcher.update(id, 0);
        }

        void update(LocalDate date) {
            id = ++track_id;
            DateTime start = date.toDateTime(new LocalTime(0, 0));
            LocalDate next = date.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            execute(WAYSTANDS,
                    api_key,
                    start.toDate().getTime() + "",
                    finish.toDate().getTime() + "");
        }
    }

    class TrackFetcher extends HttpTask {
        int id;
        int pos;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            Vector<Point> track = new Vector<Point>();
            JSONArray list = res.getJSONArray("gpslist");
            for (int i = 0; i < list.length(); i++) {
                JSONObject p = list.getJSONObject(i);
                if (!p.getBoolean("valid"))
                    continue;
                Point point = new Point();
                point.latitude = p.getDouble("latitude");
                point.longitude = p.getDouble("longitude");
                point.speed = p.getDouble("speed");
                point.time = p.getLong("eventTime");
                track.add(point);
            }
            tracks.get(pos).track = track;
            if (++pos >= tracks.size()) {
                // All tracks done
                TrackInfoFetcher fetcher = new TrackInfoFetcher();
                fetcher.update(id, 0);
                return;
            }
            TrackFetcher fetcher = new TrackFetcher();
            fetcher.update(id, pos);
        }

        void update(int track_id, int track_pos) {
            id = track_id;
            pos = track_pos;
            Track track = tracks.get(pos);
            execute(GPSLIST, api_key, track.begin + "", track.end + "");
        }
    }

    class TrackInfoFetcher extends HttpTask {
        int id;
        int pos;

        @Override
        void result(JSONObject data) throws JSONException {
            if (id != track_id)
                return;

            Track track = tracks.get(pos);
            track.mileage = data.getDouble("mileage") / 1000;
            track.avg_speed = data.getDouble("averageSpeed");
            track.max_speed = data.getDouble("maxSpeed");

            if (++pos >= tracks.size()) {
                // All tracks done
                TrackStartPositionFetcher fetcher = new TrackStartPositionFetcher();
                fetcher.update(id, 0);
                return;
            }
            TrackInfoFetcher fetcher = new TrackInfoFetcher();
            fetcher.update(id, pos);
        }

        void update(int track_id, int track_pos) {
            id = track_id;
            pos = track_pos;
            Track track = tracks.get(pos);
            execute(TELEMETRY, api_key, track.begin + "", track.end + "");
        }
    }

    abstract class TrackPositionFetcher extends HttpTask {
        int id;
        int pos;

        abstract Point getPoint(Track track);

        abstract TrackPositionFetcher create();

        abstract void process(Track track, String address);

        abstract void done();

        @Override
        void result(JSONObject data) throws JSONException {
            if (id != track_id)
                return;

            String address = null;
            Track track = tracks.get(pos);
            try {

                JSONArray res = data.getJSONArray("results");
                int i;
                for (i = 0; i < res.length(); i++) {
                    JSONObject addr = res.getJSONObject(i);
                    JSONArray types = addr.getJSONArray("types");
                    int n;
                    for (n = 0; n < types.length(); n++) {
                        if (types.getString(n).equals("street_address"))
                            break;
                    }
                    if (n < types.length())
                        break;
                }
                if (i >= res.length())
                    i = 0;

                JSONObject addr = res.getJSONObject(i);
                String[] parts = addr.getString("formatted_address").split(", ");
                JSONArray components = addr.getJSONArray("address_components");
                for (i = 0; i < components.length(); i++) {
                    JSONObject component = components.getJSONObject(i);
                    JSONArray types = component.getJSONArray("types");
                    int n;
                    for (n = 0; n < types.length(); n++) {
                        if (types.getString(n).equals("postal_code"))
                            break;
                    }
                    if (n >= types.length())
                        continue;
                    String name = component.getString("long_name");
                    for (n = 0; n < parts.length; n++) {
                        if (name.equals(parts[n]))
                            parts[n] = null;
                    }
                }

                for (i = 0; i < parts.length; i++) {
                    if (parts[i] == null)
                        continue;
                    if (address == null) {
                        address = parts[i];
                    } else {
                        address += ", ";
                        address += parts[i];
                    }
                }
            } catch (Exception e) {
                Point p = getPoint(track);
                address = p.latitude + "," + p.longitude;
            }

            process(track, address);

            if (++pos >= tracks.size()) {
                // All tracks done
                done();
                return;
            }
            TrackPositionFetcher fetcher = create();
            fetcher.update(id, pos);
        }

        void update(int track_id, int track_pos) {
            id = track_id;
            pos = track_pos;
            Track track = tracks.get(pos);
            Point p = getPoint(track);
            execute(ADDRESS_URL, p.latitude + "", p.longitude + "", Locale.getDefault().getLanguage());
        }
    }

    class TrackStartPositionFetcher extends TrackPositionFetcher {

        @Override
        Point getPoint(Track track) {
            return track.track.get(track.track.size() - 1);
        }

        @Override
        TrackPositionFetcher create() {
            return new TrackStartPositionFetcher();
        }

        @Override
        void process(Track track, String address) {
            track.start = address;
        }

        @Override
        void done() {
            TrackPositionFetcher fetcher = new TrackEndPositionFetcher();
            fetcher.update(id, 0);
        }
    }

    class TrackEndPositionFetcher extends TrackPositionFetcher {

        @Override
        Point getPoint(Track track) {
            return track.track.get(0);
        }

        @Override
        TrackPositionFetcher create() {
            return new TrackEndPositionFetcher();
        }

        @Override
        void process(Track track, String address) {
            String[] start_parts = track.start.split(", ");
            String[] finish_parts = address.split(", ");
            int s = start_parts.length - 1;
            int f = finish_parts.length - 1;

            while ((s > 2) && (f > 2)) {
                if (!start_parts[s].equals(finish_parts[f]))
                    break;
                s--;
                f--;
            }
            address = "";
            for (int i = 0; i < s; i++) {
                if (i > 0)
                    address += ", ";
                address += start_parts[i];
            }
            track.start = address;
            address = "";
            for (int i = 0; i < f; i++) {
                if (i > 0)
                    address += ", ";
                address += finish_parts[i];
            }
            track.finish = address;
        }

        @Override
        void done() {
            lvTracks.setAdapter(new TracksAdapter());
        }
    }

    class TracksAdapter extends BaseAdapter {

        int selected;

        TracksAdapter(){
            selected = -1;
        }

        @Override
        public int getCount() {
            return tracks.size();
        }

        @Override
        public Object getItem(int position) {
            return tracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.track_item, null);
            }
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            Track track = (Track) getItem(position);
            LocalDateTime begin = new LocalDateTime(track.begin);
            LocalDateTime end = new LocalDateTime(track.end);
            tvTitle.setText(begin.toString("HH:mm") + "-" + end.toString("HH:mm"));
            TextView tvMileage = (TextView) v.findViewById(R.id.mileage);
            String s = String.format(getString(R.string.mileage), track.mileage);
            tvMileage.setText(s);
            TextView tvAddress = (TextView) v.findViewById(R.id.address);
            tvAddress.setText(track.start + " - " + track.finish);
            TextView tvStatus = (TextView) v.findViewById(R.id.status);
            String text = "";
            if (position == selected){
                text = String.format(getString(R.string.short_status), track.avg_speed, track.max_speed);
            }
            tvStatus.setText(text);
            return v;
        }


    }

    class EmptyTracksAdapter extends TracksAdapter {
        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

    }

    String timeFormat(int minutes) {
        if (minutes < 60) {
            String s = getString(R.string.m_format);
            return String.format(s, minutes);
        }
        int hours = minutes / 60;
        minutes -= hours * 60;
        String s = getString(R.string.hm_format);
        return String.format(s, hours, minutes);
    }

    class Point {
        double latitude;
        double longitude;
        double speed;
        long time;
    }

    class Track {
        long begin;
        long end;
        double mileage;
        double avg_speed;
        double max_speed;
        String start;
        String finish;
        Vector<Point> track;
    }
}