package ru.shutoff.caralarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannedString;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.joda.time.LocalDateTime;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;

public class TrackView extends WebViewActivity {

    SharedPreferences preferences;
    Vector<TracksActivity.Track> tracks;

    class JsInterface {

        @JavascriptInterface
        public String getTrack() {
            TracksActivity.Track track = tracks.get(0);

            LocalDateTime begin = new LocalDateTime(track.begin);

            TracksActivity.Point p = track.track.get(track.track.size() - 1);
            StringBuilder track_data = new StringBuilder();
            track_data.append(p.latitude);
            track_data.append(",");
            track_data.append(p.longitude);
            track_data.append(",");
            track_data.append(infoMark(begin, track.start));
            for (int i = track.track.size() - 1; i >= 0; i--) {
                p = track.track.get(i);
                track_data.append("|");
                track_data.append(p.latitude);
                track_data.append(",");
                track_data.append(p.longitude);
                track_data.append(",");
                track_data.append(p.speed);
                track_data.append(",");
                track_data.append(p.time);
            }

            for (int i = 1; i < tracks.size(); i++) {
                TracksActivity.Track prev = tracks.get(i - 1);
                track = tracks.get(i);

                begin = new LocalDateTime(track.begin);
                LocalDateTime end = new LocalDateTime(prev.end);

                p = track.track.get(track.track.size() - 1);
                track_data.append("|");
                track_data.append(p.latitude);
                track_data.append(",");
                track_data.append(p.longitude);
                track_data.append(",");
                track_data.append(infoMark(end, begin, track.start));
                for (int n = track.track.size() - 1; n >= 0; n--) {
                    p = track.track.get(n);
                    track_data.append("|");
                    track_data.append(p.latitude);
                    track_data.append(",");
                    track_data.append(p.longitude);
                    track_data.append(",");
                    track_data.append(p.speed);
                    track_data.append(",");
                    track_data.append(p.time);
                }
            }

            track = tracks.get(tracks.size() - 1);
            LocalDateTime end = new LocalDateTime(track.end);
            p = track.track.get(0);
            track_data.append("|");
            track_data.append(p.latitude);
            track_data.append(",");
            track_data.append(p.longitude);
            track_data.append(",");
            track_data.append(infoMark(end, track.finish));

            return track_data.toString();
        }

        @JavascriptInterface
        public void save(double min_lat, double max_lat, double min_lon, double max_lon) {
            saveTrack(min_lat, max_lat, min_lon, max_lon, true);
        }

        @JavascriptInterface
        public void share(double min_lat, double max_lat, double min_lon, double max_lon) {
            shareTrack(min_lat, max_lat, min_lon, max_lon);
        }

        @JavascriptInterface
        public String kmh() {
            return getString(R.string.kmh);
        }

        @JavascriptInterface
        public String traffic() {
            return preferences.getBoolean("traffic", true) ? "1" : "";
        }
    }

    @Override
    String loadURL() {
        try {
            byte[] track_data;
            String file_name = getIntent().getStringExtra(Names.TRACK_FILE);
            if (file_name != null) {
                File file = new File(file_name);
                FileInputStream in = new FileInputStream(file);
                track_data = new byte[(int) file.length()];
                in.read(track_data);
                in.close();
                file.delete();
            } else {
                track_data = getIntent().getByteArrayExtra(Names.TRACK);
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(track_data);
            ObjectInput in = new ObjectInputStream(bis);
            tracks = (Vector<TracksActivity.Track>) in.readObject();
            in.close();
            bis.close();
        } catch (Exception ex) {
            finish();
        }
        webView.addJavascriptInterface(new JsInterface(), "android");
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("map_type", "").equals("OSM"))
            return "file:///android_asset/html/otrack.html";
        return "file:///android_asset/html/track.html";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(Names.TITLE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.track, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save: {
                webView.loadUrl("javascript:saveTrack()");
                break;
            }
            case R.id.share: {
                webView.loadUrl("javascript:shareTrack()");
                break;
            }
        }
        return false;
    }

    File saveTrack(double min_lat, double max_lat, double min_lon, double max_lon, boolean show_toast) {
        try {
            File path = Environment.getExternalStorageDirectory();
            if (path == null)
                path = getFilesDir();
            path = new File(path, "Tracks");
            path.mkdirs();

            long begin = 0;
            long end = 0;
            for (TracksActivity.Track track : tracks) {
                for (TracksActivity.Point p : track.track) {
                    if ((p.latitude < min_lat) || (p.latitude > max_lat) || (p.longitude < min_lon) || (p.longitude > max_lon))
                        continue;
                    if (begin == 0)
                        begin = p.time;
                    end = p.time;
                }
            }
            LocalDateTime d2 = new LocalDateTime(begin);
            LocalDateTime d1 = new LocalDateTime(end);

            String name = d2.toString("dd.MM.yy_HH.mm-") + d1.toString("HH.mm") + ".gpx";
            File out = new File(path, name);
            out.createNewFile();

            FileOutputStream f = new FileOutputStream(out);
            OutputStreamWriter ow = new OutputStreamWriter(f);
            BufferedWriter writer = new BufferedWriter(ow);

            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.append("<gpx\n");
            writer.append(" version=\"1.0\"\n");
            writer.append(" creator=\"ExpertGPS 1.1 - http://www.topografix.com\"\n");
            writer.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.append(" xmlns=\"http://www.topografix.com/GPX/1/0\"\n");
            writer.append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
            writer.append("<time>");
            LocalDateTime now = new LocalDateTime();
            writer.append(now.toString("yyyy-MM-dd'T'HH:mm:ss'Z"));
            writer.append("</time>\n");
            writer.append("<trk>\n");

            boolean trk = false;
            for (TracksActivity.Track track : tracks) {
                for (TracksActivity.Point p : track.track) {
                    if ((p.latitude < min_lat) || (p.latitude > max_lat) || (p.longitude < min_lon) || (p.longitude > max_lon)) {
                        if (trk) {
                            trk = false;
                            writer.append("</trkseg>\n");
                        }
                        continue;
                    }
                    if (!trk) {
                        trk = true;
                        writer.append("<trkseg>\n");
                    }
                    writer.append("<trkpt lat=\"" + p.latitude + "\" lon=\"" + p.longitude + "\">\n");
                    LocalDateTime t = new LocalDateTime(p.time);
                    writer.append("<time>" + t.toString("yyyy-MM-dd'T'HH:mm:ss'Z") + "</time>\n");
                    writer.append("</trkpt>\n");
                }
                if (trk)
                    writer.append("</trkseg>");
            }
            writer.append("</trk>\n");
            writer.append("</gpx>");
            writer.close();
            if (show_toast) {
                Toast toast = Toast.makeText(this, getString(R.string.saved) + " " + out.toString(), Toast.LENGTH_LONG);
                toast.show();
            }
            return out;
        } catch (Exception ex) {
            Toast toast = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
        return null;
    }

    void shareTrack(double min_lat, double max_lat, double min_lon, double max_lon) {
        File out = saveTrack(min_lat, max_lat, min_lon, max_lon, false);
        if (out == null)
            return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
        shareIntent.setType("application/gpx+xml");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
    }

    static String infoMark(LocalDateTime t, String address) {
        return "<b>" + t.toString("HH:mm") + "</b><br/>" + Html.toHtml(new SpannedString(address))
                .replaceAll(",", "&#x2C;")
                .replaceAll("\\|", "&#x7C;");
    }

    static String infoMark(LocalDateTime begin, LocalDateTime end, String address) {
        return "<b>" + begin.toString("HH:mm") + "-" + end.toString("HH:mm") + "</b><br/>" + Html.toHtml(new SpannedString(address))
                .replaceAll(",", "&#x2C;")
                .replaceAll("\\|", "&#x7C;");
    }

}
