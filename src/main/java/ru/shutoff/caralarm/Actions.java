package ru.shutoff.caralarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.Date;

public class Actions extends PreferenceActivity {

    private SharedPreferences preferences;

    private String car_id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.actions);

        car_id = getIntent().getStringExtra(Names.ID);
        if (car_id == null)
            car_id = "";

        PreferenceScreen screen = getPreferenceScreen();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final Context context = this;

        Preference pref = findPreference("internet_on");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                requestPassword(context, car_id, R.string.internet_on, R.string.internet_on_sum, "INTERNET ALL", "INTERNET ALL OK");
                return true;
            }
        });

        pref = findPreference("internet_off");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                requestPassword(context, car_id, R.string.internet_off, R.string.internet_off_sum, "INTERNET OFF", "INTERNET OFF OK");
                return true;
            }
        });

        pref = findPreference("rele1");
        if (getBoolPref(Names.CAR_RELE1)) {
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    rele1(context, car_id);
                    return true;
                }
            });
        } else {
            screen.removePreference(pref);
        }

        pref = findPreference("motor_on");
        if (getBoolPref(Names.CAR_AUTOSTART)) {
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    motorOn(context, car_id);
                    return true;
                }
            });
        } else {
            screen.removePreference(pref);
        }

        pref = findPreference("motor_off");
        if (getBoolPref(Names.CAR_AUTOSTART)) {
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    motorOff(context, car_id);
                    return true;
                }
            });
        } else {
            screen.removePreference(pref);
        }

        pref = findPreference("valet_on");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                valetOn(context, car_id);
                return true;
            }
        });

        pref = findPreference("valet_off");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                valetOff(context, car_id);
                return true;
            }
        });

        pref = findPreference("block");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                blockMotor(context, car_id);
                return true;
            }
        });

    }

    boolean getBoolPref(String name) {
        return preferences.getBoolean(name + car_id, false);
    }

    static void motorOn(Context context, String car_id) {
        requestPassword(context, car_id, R.string.motor_on, R.string.motor_on_sum, "MOTOR ON", "MOTOR ON OK");
    }

    static void motorOff(Context context, String car_id) {
        requestPassword(context, car_id, R.string.motor_off, R.string.motor_off_sum, "MOTOR OFF", "MOTOR OFF OK");
    }

    static void rele1(Context context, String car_id) {
        requestPassword(context, car_id, R.string.rele1, R.string.rele1_summary, "REL1 IMPULS", "REL1 IMPULS OK");
    }

    static void valetOff(final Context context, final String car_id) {
        requestCCode(context, car_id, R.string.valet_off, R.string.valet_off_msg, "INIT", "Main user OK", new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.VALET + car_id, false);
                Date now = new Date();
                ed.putLong(Names.INIT_TIME + car_id, now.getTime() / 1000);
                ed.remove(Names.VALET_TIME);
                ed.commit();
            }
        });
    }

    static void valetOn(final Context context, final String car_id) {
        requestCCode(context, car_id, R.string.valet_on, R.string.valet_on_msg, "VALET", "Valet OK", new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.VALET + car_id, true);
                ed.putBoolean(Names.GUARD + car_id, false);
                Date now = new Date();
                ed.putLong(Names.VALET_TIME + car_id, now.getTime() / 1000);
                ed.remove(Names.INIT_TIME);
                ed.commit();
            }
        });
    }

    static void blockMotor(Context context, String car_id) {
        requestPassword(context, car_id, R.string.block, R.string.block_msg, "BLOCK MTR", "BLOCK MTR OK");
    }

    static void requestPassword(final Context context, final String car_id, final int id_title, int id_message, final String sms, final String answer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage((id_message == 0) ? R.string.input_password : id_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = preferences.getString(Names.PASSWORD, "");
        if (password.length() > 0) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            builder.setView(inflater.inflate(R.layout.password, null));
        } else if (id_message == 0) {
            send_sms(context, car_id, sms, answer, id_title, null);
            return;
        }

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (password.length() > 0) {
                    EditText etPassword = (EditText) dialog.findViewById(R.id.passwd);
                    if (!password.equals(etPassword.getText().toString())) {
                        showMessage(context, id_title, R.string.invalid_password);
                        return;
                    }
                }
                dialog.dismiss();
                send_sms(context, car_id, sms, answer, id_title, null);
            }
        });
    }

    static private void requestCCode(final Context context, final String car_id, final int id_title, int id_message, final String sms, final String answer, final Runnable after) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage(id_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.ccode, null))
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        final Button ok = dialog.getButton(Dialog.BUTTON_POSITIVE);
        ok.setEnabled(false);
        final EditText ccode = (EditText) dialog.findViewById(R.id.ccode);
        ccode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ok.setEnabled(s.length() == 6);
            }
        });
        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                send_sms(context, car_id, ccode.getText() + " " + sms, answer, id_title, after);
            }
        });
    }

    static private void send_sms(final Context context, final String car_id, String sms, String answer, final int id_title, final Runnable after) {
        final ProgressDialog smsProgress = new ProgressDialog(context);
        smsProgress.setMessage(context.getString(id_title));
        smsProgress.show();
        final BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getStringExtra(Names.ID).equals(car_id))
                    return;
                smsProgress.dismiss();
                int result = intent.getIntExtra(Names.ANSWER, 0);
                if (result == RESULT_OK) {
                    if (after != null) {
                        after.run();
                        Intent i = new Intent(FetchService.ACTION_UPDATE);
                        i.putExtra(Names.ID, car_id);
                        context.sendBroadcast(i);
                    } else {
                        Intent i = new Intent(context, FetchService.class);
                        i.setAction(FetchService.ACTION_START);
                        i.putExtra(Names.ID, car_id);
                        context.startService(i);
                    }
                    return;
                }
                showMessage(context, id_title,
                        (result == SmsMonitor.INCORRECT_MESSAGE) ? R.string.bad_ccode : R.string.sms_error);
            }
        };
        context.registerReceiver(br, new IntentFilter(SmsMonitor.SMS_ANSWER));
        smsProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                context.unregisterReceiver(br);
            }
        });
        SmsMonitor.sendSMS(context, car_id, sms, answer);
    }

    static void showMessage(Context context, int id_title, int id_message) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage(id_message)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
    }

}
