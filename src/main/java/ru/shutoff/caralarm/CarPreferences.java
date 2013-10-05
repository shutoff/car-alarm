package ru.shutoff.caralarm;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.widget.Toast;

public class CarPreferences extends PreferenceActivity {

    SharedPreferences sPref;

    Preference smsPref;
    Preference phonePref;
    Preference apiPref;
    EditTextPreference namePref;

    ProgressDialog smsProgress;

    String car_id;

    private static final int GET_PHONE_NUMBER = 3007;
    private static final int SMS_SENT_RESULT = 3010;
    private static final int SMS_SENT_PASSWD = 3011;

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

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SMS_SENT_RESULT) {
            if (resultCode == RESULT_OK)
                return;
            if (smsProgress != null)
                smsProgress.dismiss();
            if (resultCode != Names.ANSWER_OK) {
                Toast.makeText(this, getString(R.string.sms_error), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (resultCode != RESULT_OK)
            return;
        switch (requestCode) {
            case SMS_SENT_PASSWD:
                real_smsMode();
                return;

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
        if (sPref.getString(Names.PASSWORD, "").equals("")) {
            real_smsMode();
            return;
        }
        Intent intent = new Intent(this, PasswordDialog.class);
        intent.putExtra(Names.TITLE, getString(R.string.sms_mode));
        startActivityForResult(intent, SMS_SENT_PASSWD);
    }

    void real_smsMode() {
        smsProgress = new ProgressDialog(this) {
            protected void onStop() {
                smsProgress = null;
                State.waitAnswer = null;
                State.waitAnswerPI = null;
            }
        };
        smsProgress.setMessage(getString(R.string.sms_mode));
        smsProgress.show();
        PendingIntent sendPI = createPendingResult(SMS_SENT_RESULT, new Intent(), 0);
        SmsManager smsManager = SmsManager.getDefault();
        String phoneNumber = sPref.getString(Names.CAR_PHONE + car_id, "");
        State.waitAnswer = "ALARM SMS OK";
        State.waitAnswerPI = sendPI;
        smsManager.sendTextMessage(phoneNumber, null, "ALARM SMS", sendPI, null);
    }
}
