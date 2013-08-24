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

    abstract void error();

    void background(JSONObject res) throws JSONException {
    }

    int pause = 0;
    int status;

    @Override
    protected JSONObject doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        String url = params[0];
        for (int i = 1; i < params.length; i++) {
            url = url.replace("$" + i, params[i]);
        }

        try {
            if (pause > 0)
                Thread.sleep(pause);
            HttpResponse response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            status = statusLine.getStatusCode();
            if (status != HttpStatus.SC_OK)
                return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            String res = out.toString();
            JSONObject result = new JSONObject(res);
            background(result);
            return result;
        } catch (Exception ex) {
            error();
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject res) {
        try {
            result(res);
        } catch (Exception ex) {
            error();
        }
    }

}
