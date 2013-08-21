package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class TracksActivity extends ActionBarActivity {

    SharedPreferences preferences;
    String api_key;

    TextView tvStatus;
    ProgressBar prgFirst;
    ProgressBar prgMain;
    TextView tvLoading;
    ListView lvTracks;

    CaldroidFragment dialogCaldroidFragment;
    LocalDate current;
    Vector<Track> tracks;

    int days;
    int progress;
    int track_id;

    boolean firstWay;
    boolean lastWay;
    boolean loaded;

    static final int MAX_DAYS = 7;

    final static String TELEMETRY = "http://api.car-online.ru/v2?get=telemetry&skey=$1&begin=$2&end=$3&content=json";
    final static String WAYSTANDS = "http://api.car-online.ru/v2?get=waystands&skey=$1&begin=$2&end=$3&content=json";
    final static String GPSLIST = "http://api.car-online.ru/v2?get=gpslist&skey=$1&begin=$2&end=$3&content=json";
    static final String ADDRESS_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = null;
        loaded  = false;
        if (savedInstanceState != null){
            try{
                current = new LocalDate(savedInstanceState.getLong(Names.TRACK_DATE));
                byte[] data = savedInstanceState.getByteArray(Names.TRACK);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                tracks = (Vector<Track>)ois.readObject();
                loaded = true;
            }catch (Exception ex){
                // ignore
            }
        }

        setContentView(R.layout.tracks);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        api_key = preferences.getString(Names.KEY, "");
        days = 0;
        track_id = 0;

        tvStatus  = (TextView) findViewById(R.id.status);
        lvTracks  = (ListView) findViewById(R.id.tracks);
        prgFirst  = (ProgressBar) findViewById(R.id.first_progress);
        prgMain   = (ProgressBar) findViewById(R.id.progress);
        tvLoading = (TextView) findViewById(R.id.loading);

        lvTracks.setClickable(true);
        lvTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TracksAdapter adapter = (TracksAdapter)lvTracks.getAdapter();
                if (adapter.selected == position){
                    showTrack(position);
                    return;
                }
                adapter.selected = position;
                adapter.notifyDataSetChanged();
            }
        });

        if (current != null)
            setTitle(current.toString("d MMMM"));

        if (loaded){
            all_done();
            return;
        }
        if (current != null){
            DataFetcher fetcher = new DataFetcher();
            fetcher.update(current);
            return;
        }
        DataFetcher fetcher = new FirstDataFetcher();
        fetcher.update(new LocalDate());
    }

    @Override
    protected void onSaveInstanceState (Bundle outState){
        super.onSaveInstanceState(outState);
        if (current != null)
            outState.putLong(Names.TRACK_DATE, current.toDate().getTime());
        if (!loaded)
            return;
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(tracks);
            byte[] data = baos.toByteArray();
            outState.putByteArray(Names.TRACK, data);
        }catch (Exception ex){
            // ignore
        }
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
                dialogCaldroidFragment = new CaldroidFragment() {

                    @Override
                    public void onAttach (Activity activity){
                        super.onAttach(activity);
                        CaldroidListener listener = new CaldroidListener() {

                            @Override
                            public void onSelectDate(Date date, View view) {
                                changeDate(date);
                            }
                        };

                        dialogCaldroidFragment = this;
                        setCaldroidListener(listener);
                    }

                };
                Bundle args = new Bundle();
                args.putString(CaldroidFragment.DIALOG_TITLE, getString(R.string.day));
                args.putInt(CaldroidFragment.MONTH, current.getMonthOfYear());
                args.putInt(CaldroidFragment.YEAR, current.getYear());
                args.putInt(CaldroidFragment.START_DAY_OF_WEEK, 1);
                dialogCaldroidFragment.setArguments(args);
                LocalDateTime now = new LocalDateTime();
                dialogCaldroidFragment.setMaxDate(now.toDate());
                dialogCaldroidFragment.show(getSupportFragmentManager(), "TAG");
                break;
            }
        }
        return false;
    }

    void changeDate(Date date){
        LocalDate d = new LocalDate(date);
        setTitle(d.toString("d MMMM"));
        tvStatus.setVisibility(View.GONE);
        lvTracks.setVisibility(View.GONE);
        tvLoading.setVisibility(View.VISIBLE);
        prgFirst.setVisibility(View.VISIBLE);
        prgMain.setVisibility(View.VISIBLE);
        prgMain.setProgress(0);
        DataFetcher fetcher = new DataFetcher();
        fetcher.update(d);
        dialogCaldroidFragment.dismiss();
        dialogCaldroidFragment = null;
    }

    void showTrack(int index){
        Intent intent = new Intent(this, TrackView.class);
        Track track = tracks.get(index);
        String track_data = null;
        for (int i = 0; i < track.track.size(); i++){
            Point p = track.track.get(i);
            String part = p.latitude + "," + p.longitude + "," + p.speed + "," + p.time;
            if (i > 0){
                track_data += "|";
                track_data += part;
            }else{
                track_data = part;
            }
        }
        intent.putExtra(Names.TRACK, track_data);
        intent.putExtra(Names.STATUS, String.format(getString(R.string.status),
                track.mileage,
                timeFormat((int)((track.end - track.begin) / 60000)),
                track.avg_speed,
                track.max_speed));
        LocalDateTime begin = new LocalDateTime(track.begin);
        LocalDateTime end = new LocalDateTime(track.end);
        intent.putExtra(Names.TITLE, begin.toString("d MMMM HH:mm") + "-" + end.toString("HH:mm"));
        startActivity(intent);
    }

    void showError(){
        tvStatus.setText(getString(R.string.error_load));
        tvStatus.setVisibility(View.VISIBLE);
        lvTracks.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
        prgFirst.setVisibility(View.GONE);
        prgMain.setVisibility(View.GONE);
    }

    void all_done(){
        prgFirst.setVisibility(View.GONE);
        tvStatus.setVisibility(View.VISIBLE);
        if (tracks.size() == 0){
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(getString(R.string.no_data));
            prgMain.setVisibility(View.GONE);
            tvLoading.setVisibility(View.GONE);
            return;
        }
        double mileage = 0;
        double max_speed = 0;
        long time = 0;
        long start = current.toDateTime(new LocalTime(0, 0)).toDate().getTime();
        LocalDate next = current.plusDays(1);
        long finish = next.toDateTime(new LocalTime(0, 0)).toDate().getTime();
        for (Track track: tracks){
            long begin = track.begin;
            if (begin < start)
                begin = start;
            long end = track.end;
            if (end > finish)
                end = finish;
            time += (end - begin);
            if (track.day_max_speed > max_speed)
                max_speed = track.day_max_speed;
            mileage += track.day_mileage;
        }
        double avg_speed = mileage * 3600000. / time;
        String status = getString(R.string.status);
        status = String.format(status, mileage, timeFormat((int)(time / 60000)), avg_speed, max_speed);
        tvStatus.setText(status);

        tvLoading.setVisibility(View.GONE);
        prgMain.setVisibility(View.GONE);
        lvTracks.setVisibility(View.VISIBLE);
        lvTracks.setAdapter(new TracksAdapter());

        loaded = true;
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
            if ((ways == 0) && noData())
                return;
            setTitle(date.toString("d MMMM"));
            prgFirst.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            if (ways == 0){
                all_done();
                return;
            }
            prgMain.setMax(ways * 3 + 4);
            progress = 1;
            prgMain.setProgress(1);
            double mileage = data.getDouble("mileage") / 1000;
            double avg_speed = data.getDouble("averageSpeed");
            double max_speed = data.getDouble("maxSpeed");
            int engine_time = data.getInt("engineTime") / 60000;
            String status = getString(R.string.status);
            status = String.format(status, mileage, timeFormat(engine_time), avg_speed, max_speed);
            tvStatus.setText(status);
            TracksFetcher tracksFetcher = new TracksFetcher();
            tracksFetcher.update();
        }

        @Override
        void error() {
            showError();
        }

        void update(LocalDate d) {
            date = d;
            current = d;
            loaded = false;
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
            firstWay = false;
            lastWay = false;
            for (int i = 0; i < list.length(); i++) {
                lastWay = false;
                JSONObject way = list.getJSONObject(i);
                if (!way.getString("type").equals("WAY"))
                    continue;
                JSONArray events = way.getJSONArray("events");
                int last = events.length() - 1;
                long begin = events.getJSONObject(0).getLong("eventTime");
                long end = events.getJSONObject(last).getLong("eventTime");
                if (end > begin){
                    Track track = new Track();
                    track.begin = begin;
                    track.end = end;
                    tracks.add(track);
                    if (i == 0)
                        firstWay = true;
                    lastWay = true;
                }
            }
            prgMain.setProgress(++progress);
            if (firstWay){
                PrevTracksFetcher fetcher = new PrevTracksFetcher();
                fetcher.update();
                return;
            }
            if (lastWay){
                NextTracksFetcher fetcher = new NextTracksFetcher();
                fetcher.update();
                return;
            }
            TrackFetcher fetcher = new TrackFetcher();
            fetcher.update(id, 0);
        }

        @Override
        void error() {
            showError();
        }

        void update() {
            id = ++track_id;
            DateTime start = current.toDateTime(new LocalTime(0, 0));
            LocalDate next = current.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            execute(WAYSTANDS,
                    api_key,
                    start.toDate().getTime() + "",
                    finish.toDate().getTime() + "");
        }
    }

    class PrevTracksFetcher extends  HttpTask {
        int id;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            JSONArray list = res.getJSONArray("waystandlist");
            JSONObject way = list.getJSONObject(list.length() - 1);
            if (way.getString("type").equals("WAY")){
                JSONArray events = way.getJSONArray("events");
                long begin = events.getJSONObject(0).getLong("eventTime");
                tracks.get(0).begin = begin;
            }
            prgMain.setProgress(++progress);
            if (lastWay){
                NextTracksFetcher fetcher = new NextTracksFetcher();
                fetcher.update();
                return;
            }
            TrackFetcher fetcher = new TrackFetcher();
            fetcher.update(id, 0);
        }

        @Override
        void error() {
            showError();
        }

        void update() {
            id = ++track_id;
            DateTime finish = current.toDateTime(new LocalTime(0, 0));
            LocalDate prev = current.minusDays(1);
            DateTime start = prev.toDateTime(new LocalTime(0, 0));
            execute(WAYSTANDS,
                    api_key,
                    start.toDate().getTime() + "",
                    finish.toDate().getTime() + "");
        }
    }

    class NextTracksFetcher extends  HttpTask {
        int id;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            JSONArray list = res.getJSONArray("waystandlist");
            JSONObject way = list.getJSONObject(0);
            if (way.getString("type").equals("WAY")){
                JSONArray events = way.getJSONArray("events");
                long end = events.getJSONObject(events.length() - 1).getLong("eventTime");
                tracks.get(tracks.size() - 1).end = end;
            }
            prgMain.setProgress(++progress);
            TrackFetcher fetcher = new TrackFetcher();
            fetcher.update(id, 0);
        }

        @Override
        void error() {
            showError();
        }

        void update() {
            id = ++track_id;
            LocalDate next = current.plusDays(1);
            DateTime start = next.toDateTime(new LocalTime(0, 0));
            next = next.plusDays(1);
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
            boolean first = true;
            for (int i = 0; i < list.length(); i++) {
                JSONObject p = list.getJSONObject(i);
                if (!p.getBoolean("valid"))
                    continue;
                Point point = new Point();
                point.speed = p.getDouble("speed");
                if (first){
                    if (point.speed == 0)
                        continue;
                    first = false;
                }
                point.latitude = p.getDouble("latitude");
                point.longitude = p.getDouble("longitude");
                point.time = p.getLong("eventTime");
                track.add(point);
            }
            for (int i = track.size() - 1; i >= 0; i--){
                Point p = track.get(i);
                if (p.speed > 0)
                    break;
                track.remove(i);
            }
            if (track.size() > 2){
                double distance = 0;
                double day_distance = 0;
                double max_speed = 0;
                double day_max_speed = 0;
                long start = current.toDateTime(new LocalTime(0, 0)).toDate().getTime();
                LocalDate next = current.plusDays(1);
                long finish = next.toDateTime(new LocalTime(0, 0)).toDate().getTime();
                for (int i = 0; i < track.size() - 1; i++){
                    Point p1 = track.get(i);
                    Point p2 = track.get(i + 1);
                    double d = calc_distance(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
                    distance += d;
                    if (p2.speed > max_speed)
                        max_speed = p2.speed;
                    if ((p1.time >= start) && (p2.time <= finish)){
                        day_distance += d;
                        if (p2.speed > day_max_speed)
                            day_max_speed = p2.speed;
                    }
                }
                Track t = tracks.get(pos);
                t.track   = track;
                t.mileage = distance / 1000.;
                t.max_speed = max_speed;
                t.end   = track.get(0).time;
                t.begin = track.get(track.size() - 1).time;
                t.avg_speed = distance * 3600. / (t.end - t.begin);
                t.day_mileage = day_distance / 1000.;
                t.day_max_speed = day_max_speed;
            }else{
                tracks.remove(pos--);
            }

            prgMain.setProgress(++progress);
            if (++pos >= tracks.size()) {
                // All tracks done
                TrackStartPositionFetcher fetcher = new TrackStartPositionFetcher();
                fetcher.update(id, 0);
                return;
            }
            TrackFetcher fetcher = new TrackFetcher();
            fetcher.update(id, pos);
        }

        @Override
        void error() {
            showError();
        }

        void update(int track_id, int track_pos) {
            id = track_id;
            pos = track_pos;
            Track track = tracks.get(pos);
            execute(GPSLIST, api_key, track.begin + "", track.end + "");
        }
    }

    abstract class TrackPositionFetcher extends HttpTask {
        int id;
        int pos;

        abstract Point getPoint(Track track);

        abstract TrackPositionFetcher create();

        abstract void process(String address);

        abstract void done();

        @Override
        void result(JSONObject data) throws JSONException {
            if (id != track_id)
                return;

            String address = null;
            try {
                JSONArray res = data.getJSONArray("results");
                if (res.length() == 0){
                    String status = data.getString("status");
                    if ((status != null) && status.equals("OVER_QUERY_LIMIT")){
                        TrackPositionFetcher fetcher = create();
                        fetcher.pause = 1000;
                        fetcher.update(id, pos);
                        return;
                    }
                }

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
                address = addr.getString("formatted_address");
                String[] parts = address.split(", ");
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

                address = null;
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
            } catch (Exception ex) {
                Track track = tracks.get(pos);
                Point p = getPoint(track);
                address = p.latitude + "," + p.longitude;
            }

            process(address);
            prgMain.setProgress(++progress);

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

        @Override
        void error() {
            showError();
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
        void process(String address) {
            tracks.get(pos).start = address;
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
        void process(String address) {
            Track track = tracks.get(pos);

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
            all_done();
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
                text = String.format(getString(R.string.short_status),
                        timeFormat((int) ((track.end - track.begin) / 60000)),
                        track.avg_speed,
                        track.max_speed);
                tvTitle.setTypeface(null, Typeface.BOLD);
                tvMileage.setTypeface(null, Typeface.BOLD);
            }else{
                tvTitle.setTypeface(null, Typeface.NORMAL);
                tvMileage.setTypeface(null, Typeface.NORMAL);
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

    public static class Point implements Serializable {
        double latitude;
        double longitude;
        double speed;
        long time;
    }

    public static class Track implements Serializable {
        long begin;
        long end;
        double mileage;
        double day_mileage;
        double avg_speed;
        double max_speed;
        double day_max_speed;
        String start;
        String finish;
        Vector<Point> track;
    }

    static final double D2R = 0.017453; // Константа для преобразования градусов в радианы
    static final double a = 6378137.0; // Основные полуоси
    static final double e2 = 0.006739496742337; // Квадрат эксцентричности эллипсоида

    static double calc_distance(double lat1, double lon1, double lat2, double lon2){

        if ((lat1 == lat2) && (lon1 == lon2))
            return 0;

        double fdLambda = (lon1 - lon2) * D2R;
        double fdPhi = (lat1 - lat2) * D2R;
        double fPhimean= ((lat1 + lat2) /2.0) * D2R;

        double fTemp = 1 - e2 * (Math.pow(Math.sin(fPhimean), 2));
        double fRho = (a * (1 - e2)) / Math.pow(fTemp, 1.5);
        double fNu = a / (Math.sqrt(1 - e2 * (Math.sin(fPhimean) * Math.sin(fPhimean))));

        double fz = Math.sqrt(Math.pow(Math.sin(fdPhi / 2.0), 2) +
                Math.cos(lat2 * D2R) * Math.cos(lat1 * D2R) * Math.pow(Math.sin(fdLambda / 2.0), 2));
        fz = 2 * Math.asin(fz);

        double fAlpha = Math.cos(lat1 * D2R) * Math.sin(fdLambda) * 1 / Math.sin(fz);
        fAlpha = Math.asin(fAlpha);

        double fR = (fRho * fNu) / ((fRho * Math.pow(Math.sin(fAlpha), 2)) + (fNu * Math.pow(Math.cos(fAlpha), 2)));

        return fz * fR;
    }

}
