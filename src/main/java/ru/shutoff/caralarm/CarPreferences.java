package ru.shutoff.caralarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;

public class CarPreferences extends PreferenceActivity {

    SharedPreferences sPref;

    Preference smsPref;
    Preference phonePref;
    Preference apiPref;
    EditTextPreference namePref;
    SeekBarPreference shiftPref;

    String car_id;

    private static final int GET_PHONE_NUMBER = 3007;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);

        car_id = getIntent().getStringExtra(Names.ID);
        if (car_id == null)
            car_id = "";

        String title = sPref.getString(Names.CAR_NAME + car_id, "");
        String name = title;
        if (title.equals("")) {
            title = getString(R.string.new_car);
            name = getString(R.string.car);
            if (!car_id.equals(""))
                name += " " + car_id;
        }
        setTitle(title);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt("tmp_shift", sPref.getInt(Names.TEMP_SIFT + car_id, 0));
        ed.putString("name_", name);
        ed.commit();

        addPreferencesFromResource(R.xml.car_preferences);

        namePref = (EditTextPreference) findPreference("name_");
        namePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = newValue.toString();
                setTitle(value);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putString(Names.CAR_NAME + car_id, value);
                ed.commit();
                namePref.setSummary(value);
                return true;
            }
        });
        namePref.setSummary(name);

        smsPref = findPreference("sms_mode");
        smsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                smsMode();
                return true;
            }
        });

        phonePref = findPreference("phone");
        phonePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(new Intent(getBaseContext(), ContactsPickerActivity.class),
                        GET_PHONE_NUMBER);
                return true;
            }
        });
        String phoneNumber = sPref.getString(Names.CAR_PHONE + car_id, "");
        setPhone(phoneNumber);

        apiPref = findPreference("api_key");
        apiPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), ApiKeyDialog.class);
                intent.putExtra(Names.ID, car_id);
                startActivity(intent);
                return true;
            }
        });

        shiftPref = (SeekBarPreference) findPreference("tmp_shift");
        shiftPref.setMin(-10);
        shiftPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Integer) {
                    int v = (Integer) newValue;
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putInt(Names.TEMP_SIFT + car_id, v);
                    ed.commit();
                    Intent intent = new Intent(FetchService.ACTION_UPDATE);
                    intent.putExtra(Names.ID, car_id);
                    try {
                        getBaseContext().sendBroadcast(intent);
                    } catch (Exception ex) {
                        // ignore
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        switch (requestCode) {
            case GET_PHONE_NUMBER: {
                String phoneNumber = (String) data.getExtras().get(
                        ContactsPickerActivity.KEY_PHONE_NUMBER);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putString(Names.CAR_PHONE + car_id, phoneNumber);
                ed.commit();
                setPhone(phoneNumber);
                break;
            }
        }
    }

    void setPhone(String phoneNumber) {
        if (phoneNumber.length() > 0) {
            phonePref.setSummary(phoneNumber);
            smsPref.setEnabled(true);
        } else {
            smsPref.setEnabled(false);
        }
    }

    void smsMode() {
        Actions.requestPassword(this, car_id, R.string.sms_mode, R.string.sms_mode_msg, "ALARM SMS", "ALARM SMS OK");
    }

}
