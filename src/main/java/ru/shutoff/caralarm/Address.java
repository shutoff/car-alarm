package ru.shutoff.caralarm;

import android.content.SharedPreferences;

public abstract class Address {
    abstract void onResult();

    SharedPreferences preferences;

    AddressRequest request;

    String latitude;
    String longitude;
    String car_id;

    Address(SharedPreferences p) {
        preferences = p;
    }

    void update(String id) {
        car_id = id;
        latitude = preferences.getString(Names.LATITUDE + car_id, null);
        longitude = preferences.getString(Names.LONGITUDE + car_id, null);
        if ((latitude == null) || (longitude == null))
            return;
        String addr_lat = preferences.getString(Names.ADDR_LAT + car_id, "");
        String addr_lon = preferences.getString(Names.ADDR_LNG + car_id, "");
        if (addr_lat.equals(latitude) && addr_lon.equals(longitude))
            return;
        if (request != null)
            return;
        request = new AddressRequest() {
            @Override
            void addressResult(String[] parts) {
                request = null;
                if (parts == null)
                    return;

                String address = parts[0];
                for (int i = 1; i < parts.length - 1; i++) {
                    address += ", " + parts[i];
                }
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.ADDR_LAT + car_id, latitude);
                ed.putString(Names.ADDR_LNG + car_id, longitude);
                ed.putString(Names.ADDRESS + car_id, address);
                ed.commit();
                onResult();
            }

        };
        request.getAddress(preferences, latitude, longitude);
    }

}
