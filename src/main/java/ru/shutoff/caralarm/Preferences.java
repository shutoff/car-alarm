package ru.shutoff.caralarm;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

    SharedPreferences sPref;
    Preference phonePref;
    Preference alarmPref;
    Preference notifyPref;
    Preference smsPref;
    Preference apiPref;
    Preference testPref;
    String alarmUri;
    String notifyUri;
    ProgressDialog smsProgress;

    private static final int GET_PHONE_NUMBER = 3007;
    private static final int GET_ALARM_SOUND = 3008;
    private static final int GET_NOTIFY_SOUND = 3009;
    private static final int SMS_SENT_RESULT = 3010;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.preferences);

        smsPref = (Preference) findPreference("sms_mode");
        smsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                smsMode();
                return true;
            }
        });

        phonePref = (Preference) findPreference("phone");
        phonePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(new Intent(getBaseContext(), ContactsPickerActivity.class),
                        GET_PHONE_NUMBER);
                return true;
            }
        });
        String phoneNumber = sPref.getString(Names.PHONE, "");
        setPhone(phoneNumber);

        alarmPref = (Preference) findPreference("alarm");
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

        notifyPref = (Preference) findPreference("notify");
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

        apiPref = (Preference) findPreference("api_key");
        apiPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), ApiKeyDialog.class);
                startActivity(intent);
                return true;
            }
        });

        testPref = (Preference) findPreference("alarm_test");
        testPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), Alarm.class);
                intent.putExtra(Names.ALARM, getString(R.string.alarm_test));
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
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
            case GET_PHONE_NUMBER: {
                String phoneNumber = (String) data.getExtras().get(
                        ContactsPickerActivity.KEY_PHONE_NUMBER);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putString(Names.PHONE, phoneNumber);
                ed.commit();
                setPhone(phoneNumber);
                break;
            }
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


    void setPhone(String phoneNumber) {
        if (phoneNumber.length() > 0) {
            phonePref.setSummary(phoneNumber);
            smsPref.setEnabled(true);
        } else {
            smsPref.setEnabled(false);
        }
    }

    void smsMode() {
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
        String phoneNumber = sPref.getString(Names.PHONE, "");
        State.waitAnswer = "ALARM SMS OK";
        State.waitAnswerPI = sendPI;
        smsManager.sendTextMessage(phoneNumber, null, "ALARM SMS", sendPI, null);
    }
}
