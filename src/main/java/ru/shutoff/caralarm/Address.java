package ru.shutoff.caralarm;

import android.content.SharedPreferences;

public abstract class Address {
    abstract void onResult();

    SharedPreferences preferences;

    AddressRequest request;

    String latitude;
    String longitude;

    Address(SharedPreferences p) {
        preferences = p;
    }

    void update() {
        latitude = preferences.getString(Names.Latitude, null);
        longitude = preferences.getString(Names.Longitude, null);
        if ((latitude == null) || (longitude == null))
            return;
        String addr_lat = preferences.getString(Names.AddrLatitude, "");
        String addr_lon = preferences.getString(Names.AddrLongitude, "");
//        if (addr_lat.equals(latitude) && addr_lon.equals(longitude))
//            return;
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
                ed.putString(Names.AddrLatitude, latitude);
                ed.putString(Names.AddrLongitude, longitude);
                ed.putString(Names.Address, address);
                ed.commit();
                onResult();
            }

        };
        request.getAddress(preferences, latitude, longitude);
    }

}
