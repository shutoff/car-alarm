package ru.shutoff.caralarm;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

abstract public class AddressRequest {

    static final String GOOGLE_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";
    static final String YANDEX_URL = "http://geocode-maps.yandex.ru/1.x/?geocode=$2,$1&format=json&lang=$3";

    abstract void addressResult(String[] address);

    Request request;

    void getAddress(SharedPreferences preferences, String lat, String lng) {
        String map_type = preferences.getString(Names.MAP_TYPE, "Google");
        if (map_type.equals("Yandex")) {
            request = new YandexRequest();
        } else {
            request = new GoogleRequest();
        }
        request.exec(lat, lng);
    }

    abstract class Request extends HttpTask {
        abstract void exec(String lat, String lng);
    }

    class GoogleRequest extends Request {

        void exec(String lat, String lng) {
            latitude = lat;
            longitude = lng;
            execute(GOOGLE_URL, latitude, longitude, Locale.getDefault().getLanguage());
        }

        String latitude;
        String longitude;

        @Override
        void result(JSONObject data) throws JSONException {
            if (data == null)
                return;

            JSONArray res = data.getJSONArray("results");
            if (res.length() == 0) {
                String status = data.getString("status");
                if ((status != null) && status.equals("OVER_QUERY_LIMIT")) {
                    request = new GoogleRequest();
                    request.pause = 1000;
                    request.exec(latitude, longitude);
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
            int to = 0;
            for (i = 0; i < parts.length; i++) {
                if (parts[i] == null)
                    continue;
                to++;
            }
            String[] p = new String[to];
            to = 0;
            for (i = 0; i < parts.length; i++) {
                if (parts[i] == null)
                    continue;
                p[to++] = parts[i];
            }
            addressResult(p);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }

    class YandexRequest extends Request {

        String latitude;
        String longitude;
        boolean second;

        @Override
        void exec(String lat, String lng) {
            latitude = lat;
            longitude = lng;
            execute(YANDEX_URL + "&kind=house", latitude, longitude, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JSONObject res) throws JSONException {
            res = res.getJSONObject("response");
            res = res.getJSONObject("GeoObjectCollection");
            JSONArray results = res.getJSONArray("featureMember");
            if (results.length() == 0) {
                if (second){
                    addressResult(null);
                    return;
                }
                YandexRequest r = new YandexRequest();
                r.second = true;
                request = r;
                execute(YANDEX_URL, latitude, longitude, Locale.getDefault().getLanguage());
                return;
            }
            res = results.getJSONObject(0);
            res = res.getJSONObject("GeoObject");
            String[] name = res.getString("name").split(", ");
            String[] desc = res.getString("description").split(", ");
            String[] parts = new String[name.length + desc.length];
            System.arraycopy(name, 0, parts, 0, name.length);
            System.arraycopy(desc, 0, parts, name.length, desc.length);
            addressResult(parts);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }
}
