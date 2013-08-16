package ru.shutoff.caralarm;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public abstract class Address {
    abstract void onResult();

    SharedPreferences preferences;

    static final String URL = "http://nominatim.openstreetmap.org/reverse?lat=$1&lon=$2&format=json&address_details=1&accept-language=$3";

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
                String address = getString(data, "display_name");
                if (address == null)
                    return;
                String[] parts = address.split(", ");
                data = data.getJSONObject("address");
                if ((getString(data, "house_number") != null) && (parts.length > 2)){
                    String r = parts[0];
                    parts[0] = parts[1];
                    parts[1] = r;
                }
                remove_part(parts, getString(data, "postcode"));
                remove_part(parts, getString(data, "country"));
                address = null;
                for (String part: parts){
                    if (part == null)
                        continue;
                    if (address == null){
                        address = part;
                        continue;
                    }
                    address += ", ";
                    address += part;
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

    static void remove_part(String[] parts, String part){
        if (part == null)
            return;
        for (int i = 0; i < parts.length; i++){
            if (part.equals(parts[i])){
                parts[i] = null;
                break;
            }
        }
    }

}
