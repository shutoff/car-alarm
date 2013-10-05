package ru.shutoff.caralarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

    SharedPreferences sPref;
    Preference alarmPref;
    Preference notifyPref;
    Preference testPref;
    Preference aboutPref;
    Preference pswdPref;
    ListPreference mapPref;

    String alarmUri;
    String notifyUri;

    private static final int GET_ALARM_SOUND = 3008;
    private static final int GET_NOTIFY_SOUND = 3009;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.preferences);

        alarmPref = findPreference("alarm");
        alarmPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                if (alarmUri != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmUri));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }

                startActivityForResult(intent, GET_ALARM_SOUND);
                return true;
            }
        });

        alarmUri = sPref.getString(Names.ALARM, "");
        setAlarmTitle();

        notifyPref = findPreference("notify");
        notifyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                if (notifyUri != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(notifyUri));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }

                startActivityForResult(intent, GET_NOTIFY_SOUND);
                return true;
            }
        });

        notifyUri = sPref.getString(Names.NOTIFY, "");
        setNotifyTitle();

        aboutPref = findPreference("about");
        try {
            PackageManager pkgManager = getPackageManager();
            PackageInfo info = pkgManager.getPackageInfo("ru.shutoff.caralarm", 0);
            aboutPref.setSummary(aboutPref.getSummary() + " " + info.versionName);
        } catch (Exception ex) {
            aboutPref.setSummary("");
        }
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), About.class);
                startActivity(intent);
                return true;
            }
        });

        pswdPref = findPreference("password");
        pswdPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), SetPasswordDialog.class);
                startActivity(intent);
                return true;
            }
        });

        mapPref = (ListPreference) findPreference("map_type");
        mapPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mapPref.setSummary(newValue.toString());
                return true;
            }
        });

        mapPref.setSummary(sPref.getString("map_type", "Google"));

        testPref = findPreference("alarm_test");
        testPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), Alarm.class);
                intent.putExtra(Names.ALARM, getString(R.string.alarm_test));
                startActivity(intent);
                return true;
            }
        });

        Preference carPref = findPreference("cars");
        carPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), Cars.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case GET_ALARM_SOUND: {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    alarmUri = uri.toString();
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putString(Names.ALARM, alarmUri);
                    ed.commit();
                    setAlarmTitle();
                }
                break;
            }
            case GET_NOTIFY_SOUND: {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    notifyUri = uri.toString();
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putString(Names.NOTIFY, notifyUri);
                    ed.commit();
                    setNotifyTitle();
                }
                break;
            }
            default:
                break;
        }
    }

    void setAlarmTitle() {
        Uri uri = Uri.parse(alarmUri);
        Ringtone ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        if (ringtone == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        }
        if (ringtone != null) {
            String name = ringtone.getTitle(getBaseContext());
            alarmPref.setSummary(name);
        }
    }

    void setNotifyTitle() {
        Uri uri = Uri.parse(notifyUri);
        Ringtone ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        if (ringtone == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        }
        if (ringtone != null) {
            String name = ringtone.getTitle(getBaseContext());
            notifyPref.setSummary(name);
        }
    }

    static String getCar(SharedPreferences preferences, String car_id) {
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        if (car_id != null) {
            boolean car_ok = false;
            for (String car : cars) {
                if (car.equals(car_id)) {
                    car_ok = true;
                    break;
                }

            }
            if (!car_ok)
                car_id = null;
        }
        if (car_id == null)
            car_id = cars[0];
        return car_id;
    }

}
