package ru.shutoff.caralarm;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.widget.Toast;

public class Actions extends PreferenceActivity {

    static final int SMS_SENT_RESULT = 3012;

    static final int VALET_ON = 4000;
    static final int VALET_OFF = 4001;

    ProgressDialog smsProgress;

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.actions);

        Preference pref = (Preference) findPreference("internet_on");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendSMS("INTERNET ALL", "INTERNET ALL OK", getString(R.string.internet_on));
                return true;
            }
        });

        pref = (Preference) findPreference("internet_off");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendSMS("INTERNET OFF", "INTERNET OFF OK", getString(R.string.internet_off));
                return true;
            }
        });

        pref = (Preference) findPreference("motor_on");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendSMS("MOTOR ON", "MOTOR ON OK", getString(R.string.motor_on));
                return true;
            }
        });

        pref = (Preference) findPreference("motor_off");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendSMS("MOTOR OFF", "MOTOR OFF OK", getString(R.string.motor_off));
                return true;
            }
        });

        pref = (Preference) findPreference("valet_on");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getCCode(getBaseContext().getString(R.string.valet_on), VALET_ON);
                return true;
            }
        });

        pref = (Preference) findPreference("valet_off");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getCCode(getBaseContext().getString(R.string.valet_off), VALET_OFF);
                return true;
            }
        });

        pref = (Preference) findPreference("block");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendSMS("BLOCK MTR", "BLOCK MTR OK", getString(R.string.block));
                return true;
            }
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
        if (data == null)
            return;

        String cCode = data.getStringExtra(Names.CCODE);
        if ((cCode == null) || (cCode.length() == 0))
            return;
        if (requestCode == VALET_ON)
            sendSMS(cCode + " VALET", "Valet OK", getString(R.string.valet_on));
        if (requestCode == VALET_OFF)
            sendSMS(cCode + " INIT", "Main user OK", getString(R.string.valet_off));
    }

    void getCCode(String title, int id) {
        Intent intent = new Intent(this, CCodeDialog.class);
        intent.putExtra(Names.TITLE, title);
        startActivityForResult(intent, id);
    }

    void sendSMS(String text, String answer, String title) {
        smsProgress = new ProgressDialog(this) {
            protected void onStop() {
                smsProgress = null;
                State.waitAnswer = null;
                State.waitAnswerPI = null;
            }
        };
        smsProgress.setMessage(title);
        smsProgress.show();
        PendingIntent sendPI = createPendingResult(SMS_SENT_RESULT, new Intent(), 0);
        SmsManager smsManager = SmsManager.getDefault();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String phoneNumber = preferences.getString(Names.PHONE, "");
        State.waitAnswer = answer;
        State.waitAnswerPI = sendPI;
        smsManager.sendTextMessage(phoneNumber, null, text, sendPI, null);
    }

}
