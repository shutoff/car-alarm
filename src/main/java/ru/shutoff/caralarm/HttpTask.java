package ru.shutoff.caralarm;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public abstract class HttpTask extends AsyncTask<String, Void, JSONObject> {

    abstract void result(JSONObject res) throws JSONException;

    @Override
    protected JSONObject doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        String url = params[0];
        for (int i = 1; i < params.length; i++) {
            url = url.replace("$" + i, params[i]);
        }

        try {
            HttpResponse response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            if (status != HttpStatus.SC_OK)
                return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            String res = out.toString();
            return new JSONObject(res);
        } catch (Exception error) {
            // ignore
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject res) {
        try{
            result(res);
        }catch (Exception ex){
            // ignore
        }
    }

}
