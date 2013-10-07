package ru.shutoff.caralarm;

import android.content.SharedPreferences;

public abstract class Address {
    abstract void onResult();

    SharedPreferences preferences;

    Request request;

    String latitude;
    String longitude;
    String car_id;

    Address(SharedPreferences p) {
        preferences = p;
    }

    class Request extends AddressRequest {

        String id;

        Request(String car_id) {
            id = car_id;
        }

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
            if (id.equals(car_id))
                onResult();
        }
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
        request = new Request(car_id);
        request.getAddress(preferences, latitude, longitude);
    }

    static final double D2R = 0.017453; // Константа для преобразования градусов в радианы
    static final double a = 6378137.0; // Основные полуоси
    static final double e2 = 0.006739496742337; // Квадрат эксцентричности эллипсоида

    static double calc_distance(double lat1, double lon1, double lat2, double lon2) {

        if ((lat1 == lat2) && (lon1 == lon2))
            return 0;

        double fdLambda = (lon1 - lon2) * D2R;
        double fdPhi = (lat1 - lat2) * D2R;
        double fPhimean = ((lat1 + lat2) / 2.0) * D2R;

        double fTemp = 1 - e2 * (Math.pow(Math.sin(fPhimean), 2));
        double fRho = (a * (1 - e2)) / Math.pow(fTemp, 1.5);
        double fNu = a / (Math.sqrt(1 - e2 * (Math.sin(fPhimean) * Math.sin(fPhimean))));

        double fz = Math.sqrt(Math.pow(Math.sin(fdPhi / 2.0), 2) +
                Math.cos(lat2 * D2R) * Math.cos(lat1 * D2R) * Math.pow(Math.sin(fdLambda / 2.0), 2));
        fz = 2 * Math.asin(fz);

        double fAlpha = Math.cos(lat1 * D2R) * Math.sin(fdLambda) * 1 / Math.sin(fz);
        fAlpha = Math.asin(fAlpha);

        double fR = (fRho * fNu) / ((fRho * Math.pow(Math.sin(fAlpha), 2)) + (fNu * Math.pow(Math.cos(fAlpha), 2)));

        return fz * fR;
    }

    static String getAddress(SharedPreferences preferences, String car_id) {
        try {
            double lat1 = Double.parseDouble(preferences.getString(Names.LATITUDE + car_id, "0"));
            double lng1 = Double.parseDouble(preferences.getString(Names.LONGITUDE + car_id, "0"));
            double lat2 = Double.parseDouble(preferences.getString(Names.ADDR_LAT + car_id, "0"));
            double lng2 = Double.parseDouble(preferences.getString(Names.ADDR_LNG + car_id, "0"));
            if (calc_distance(lat1, lng1, lat2, lng2) < 200)
                return preferences.getString(Names.ADDRESS, "");
        } catch (Exception ex) {
            // ignore
        }
        return "";
    }
}
