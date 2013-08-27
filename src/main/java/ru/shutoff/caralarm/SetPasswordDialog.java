package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SetPasswordDialog extends Activity {

    SharedPreferences preferences;
    String old_password;

    EditText etOldPswd;
    EditText etPasswd1;
    EditText etPasswd2;
    TextView tvConfrim;
    TextView tvPswdError;
    Button btnSave;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setpassword);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        old_password = preferences.getString(Names.PASSWORD, "");
        etOldPswd = (EditText) findViewById(R.id.old_password);
        tvPswdError = (TextView) findViewById(R.id.invalid_password);
        if (old_password.equals("")) {
            TextView tvOldLabel = (TextView) findViewById(R.id.old_password_label);
            tvOldLabel.setVisibility(View.GONE);
            etOldPswd.setVisibility(View.GONE);
            tvPswdError.setVisibility(View.GONE);
        } else {
            etOldPswd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    tvPswdError.setVisibility(View.INVISIBLE);
                }
            });
        }
        etPasswd1 = (EditText) findViewById(R.id.password);
        etPasswd2 = (EditText) findViewById(R.id.password1);
        tvConfrim = (TextView) findViewById(R.id.invalid_confirm);
        btnSave = (Button) findViewById(R.id.action);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if ((etPasswd1.getText() + "").equals(etPasswd2.getText() + "")) {
                    tvConfrim.setVisibility(View.INVISIBLE);
                    btnSave.setEnabled(true);
                } else {
                    tvConfrim.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(false);
                }
            }
        };
        etPasswd1.addTextChangedListener(watcher);
        etPasswd2.addTextChangedListener(watcher);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((etPasswd1.getText() + "").equals(etPasswd2.getText() + "")) {
                    if ((etOldPswd.getText() + "").equals(preferences.getString(Names.PASSWORD, ""))) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.PASSWORD, etPasswd1.getText() + "");
                        ed.commit();
                        finish();
                    } else {
                        tvPswdError.setVisibility(View.VISIBLE);
                        etOldPswd.requestFocus();
                    }
                }
            }
        });
    }

}
