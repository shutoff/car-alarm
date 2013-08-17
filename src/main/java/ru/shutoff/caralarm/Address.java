package ru.shutoff.caralarm;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public abstract class Address {
    abstract void onResult();

    SharedPreferences preferences;

    static final String URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";

    HttpTask httpTask;

    String latitude;
    String longitude;

    Address(SharedPreferences p){
        preferences = p;
    }

    void update() {
        latitude = preferences.getString(Names.Latitude, null);
        longitude = preferences.getString(Names.Longitude, null);
        if ((latitude == null) || (longitude == null))
            return;
        String addr_lat = preferences.getString(Names.AddrLatitude, "");
        String addr_lon = preferences.getString(Names.AddrLongitude, "");
        if (addr_lat.equals(latitude) && addr_lon.equals(longitude))
            return;
        if (httpTask != null)
            return;
        httpTask = new HttpTask() {
            @Override
            void result(JSONObject data) throws JSONException {
                httpTask = null;
                if (data == null)
                    return;

                String address = null;
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
                        if (types.getString(n).equals("postal_code") ||
                            types.getString(n).equals("country"))
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

                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.AddrLatitude, latitude);
                ed.putString(Names.AddrLongitude, longitude);
                ed.putString(Names.Address, address);
                ed.commit();
                onResult();
            }
        };
        httpTask.execute(URL, latitude, longitude, Locale.getDefault().getLanguage());
    }

}
