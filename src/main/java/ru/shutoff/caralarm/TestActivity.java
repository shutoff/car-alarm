package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public class TestActivity extends Activity {

    TextView tvOut;

    final String ContactData = "contact_data";

    class HttpTask extends AsyncTask<Void, Void, Void> {

        String text;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            HttpClient httpclient = new DefaultHttpClient();
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String prev_data = preferences.getString(ContactData, "");

                String url = "http://api.car-online.ru/v2?get=lastinfo&skey=";
                url += preferences.getString(Names.KEY, "");
                url += "&content=json";
                HttpResponse response = httpclient.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                int status = statusLine.getStatusCode();
                if (status != HttpStatus.SC_OK)
                    return null;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String res = out.toString();
                JSONObject json = new JSONObject(res);
                json = json.getJSONObject("contact");

                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(ContactData, json.toString());
                ed.commit();

                if (prev_data.equals("")){
                    text = "Saved";
                    return null;
                }

                JSONObject prev = new JSONObject(prev_data);

                Iterator<String> keys = json.keys();
                text = "";
                while(keys.hasNext()){
                    String key = keys.next();
                    String val = json.getString(key);
                    if(val != null){
                        String prev_val = prev.getString(key);
                        if (!val.equals(prev_val)){
                            text += key;
                            text += ": ";
                            text += val;
                            text += "\n";
                        }
                    }
                }
                text += "________________________________";

            } catch (Exception e) {
                text = "Error: " + e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            tvOut.setText(text);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);

        tvOut = (TextView) findViewById(R.id.out);

        HttpTask task = new HttpTask();
        task.execute();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences: {
                tvOut.setText("Refresh");
                HttpTask task = new HttpTask();
                task.execute();
                break;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.test, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
