package ru.shutoff.caralarm;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class TrackView  extends ActionBarActivity {

    String track;
    WebView mapView;
    TextView tvStatus;

    class JsInterface {

        @JavascriptInterface
        public String getTrack() {
            return track;
        }

        @JavascriptInterface
        public void save(double min_lat, double max_lat, double min_lon, double max_lon) {
            saveTrack(min_lat, max_lat, min_lon, max_lon);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        track = getIntent().getStringExtra(Names.TRACK);
        setContentView(R.layout.maps);
        mapView = (WebView) findViewById(R.id.webview);
        tvStatus = (TextView) findViewById(R.id.address);
        tvStatus.setText(getIntent().getStringExtra(Names.STATUS));
        setTitle(getIntent().getStringExtra(Names.TITLE));
        WebSettings settings = mapView.getSettings();
        settings.setJavaScriptEnabled(true);
        mapView.addJavascriptInterface(new JsInterface(), "android");
        mapView.loadUrl("file:///android_asset/html/track.html");
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
                mapView.loadUrl("javascript:saveTrack()");
                break;
            }
        }
        return false;
    }

    void saveTrack(double min_lat, double max_lat, double min_lon, double max_lon){
        try{
            File path = getFilesDir();
            path = new File(path, "Tracks");
            path.mkdirs();
            File out = new File(path, getTitle() + ".kml");
            out.createNewFile();
            FileOutputStream f = new FileOutputStream(out);
            OutputStreamWriter ow = new OutputStreamWriter(f);
            BufferedWriter writer = new BufferedWriter(ow);

            String[] points = track.split("\\|");
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            writer.append("<Folder>\n");
            for (String point:points){
                String[] data = point.split(",");
                double lat = Double.parseDouble(data[0]);
                double lon = Double.parseDouble(data[1]);
                if ((lat < min_lat) || (lat > max_lat))
                    continue;
                if ((lon < min_lon) || (lon > max_lon))
                    continue;
                writer.append("<Placemark>\n");
                writer.append("<Point>\n");
                writer.append("<coordinates>");
                writer.append(lat + "," + lon + ",0");
                writer.append("</coordinates>");
                writer.append("</Point>");
                writer.append("</Placemark>");
            }
            writer.append("</Folder>");
            writer.append("</kml>");
            f.close();
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT);
        }catch (Exception ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG);
        }
    }
}
