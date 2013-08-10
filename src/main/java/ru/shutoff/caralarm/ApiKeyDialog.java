package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ApiKeyDialog extends Activity {

    EditText etKey;
    SharedPreferences preferences;
    Button btnSave;
    TextView tvMessage;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.apikeydialog);
        etKey = (EditText) findViewById(R.id.api_key);
        btnSave = (Button) findViewById(R.id.save);
        tvMessage = (TextView) findViewById(R.id.message);

        btnSave.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.KEY, etKey.getText().toString());
                ed.commit();
                finish();
            }

        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        etKey.setText(preferences.getString(Names.KEY, ""));
        etKey.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                setKeyState();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String key = bundle.getString(Names.KEY);
            if (key != null)
                etKey.setText(key);
        }
        setKeyState();
    }

    void setKeyState() {
        if (etKey.getText().toString().matches("[0-9A-Fa-f]{30}")) {
            btnSave.setEnabled(true);
            tvMessage.setText("");
        } else {
            btnSave.setEnabled(false);
            if (etKey.getText().length() == 0) {
                tvMessage.setText("");
            } else {
                tvMessage.setText(getString(R.string.bad_key));
            }
        }
    }


}
