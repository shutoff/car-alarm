package ru.shutoff.caralarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class CCodeDialog extends Activity {

    Button btnAction;
    EditText etCode;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ccode);
        btnAction = (Button) findViewById(R.id.action);
        etCode = (EditText) findViewById(R.id.ccode);
        btnAction.setText(getIntent().getStringExtra(Names.TITLE));

        btnAction.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(Names.CCODE, etCode.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
            }

        });
    }

}
