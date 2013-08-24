package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PasswordDialog extends Activity {

    Button btnAction;
    EditText etPasswd;
    TextView tvError;

    String text;
    String answer;
    String title;

    SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        btnAction = (Button) findViewById(R.id.action);
        etPasswd = (EditText) findViewById(R.id.passwd);
        tvError = (TextView) findViewById(R.id.invalid_password);

        etPasswd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                tvError.setVisibility(View.INVISIBLE);
            }
        });

        title = getIntent().getStringExtra(Names.TITLE);
        text = getIntent().getStringExtra(Names.TEXT);
        answer = getIntent().getStringExtra(Names.ANSWER);

        btnAction.setText(title);

        btnAction.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if ((etPasswd.getText() + "").equals(preferences.getString(Names.PASSWORD, ""))) {
                    Intent intent = new Intent();
                    intent.putExtra(Names.TITLE, title);
                    intent.putExtra(Names.TEXT, text);
                    intent.putExtra(Names.ANSWER, answer);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                tvError.setVisibility(View.VISIBLE);
            }

        });

    }

}
