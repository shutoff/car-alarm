package ru.shutoff.caralarm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class ApiKeyDialog extends Activity {

    EditText etKey;
    SharedPreferences preferences;
    Button btnSave;
    TextView tvMessage;
    ProgressDialog dlgCheck;
    String car_id;
    Spinner spCars;
    Cars.Car[] cars;

    final String TEST_URL = "http://api.car-online.ru/v2?get=profile&skey=$1&content=json";

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
        spCars = (Spinner) findViewById(R.id.cars);

        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        car_id = getIntent().getStringExtra(Names.ID);
        if (car_id != null) {
            spCars.setVisibility(View.GONE);
            etKey.setText(preferences.getString(Names.CAR_KEY + car_id, ""));
        } else {
            cars = Cars.getCars(this);
            if ((cars.length > 1) || (preferences.getString(Names.CAR_KEY + cars[0].id, "").length() > 0)) {
                Cars.Car[] new_cars = new Cars.Car[cars.length + 1];
                System.arraycopy(cars, 0, new_cars, 0, cars.length);
                Cars.Car new_car = new Cars.Car();
                new_car.id = "";
                new_car.name = getString(R.string.new_car);
                for (int i = 1; ; i++) {
                    if (!isId(i + "")) {
                        new_car.id = i + "";
                        break;
                    }
                }
                new_cars[cars.length] = new_car;
                cars = new_cars;
            }
            spCars.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return cars.length;
                }

                @Override
                public Object getItem(int position) {
                    return cars[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getBaseContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.car_key_item, null);
                    }
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(cars[position].name);
                    return v;
                }
            });
            spCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    car_id = cars[position].id;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spCars.setSelection(cars.length - 1);
            etKey.setText(getIntent().getStringExtra(Names.CAR_KEY));
        }

        btnSave.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                HttpTask checkCode = new HttpTask() {
                    @Override
                    void result(JSONObject res) throws JSONException {
                        if (dlgCheck != null) {
                            dlgCheck.dismiss();
                            dlgCheck = null;
                        }
                        if (res != null) {
                            res.getInt("id");
                            saveKey();
                            return;
                        }
                        showError(error_text);
                    }

                    @Override
                    void error() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showError(error_text);
                            }
                        });
                    }
                };

                checkCode.execute(TEST_URL, etKey.getText().toString());
                showProgress();
            }

        });

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
            String key = bundle.getString(Names.CAR_KEY);
            if (key != null)
                etKey.setText(key);
        }
        setKeyState();
    }

    boolean isId(String id) {
        for (Cars.Car car : cars) {
            if (car.id.equals(id))
                return true;
        }
        return false;
    }

    void saveKey() {
        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(Names.CAR_KEY + car_id, etKey.getText().toString());
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        boolean is_new = true;
        for (String car : cars) {
            if (car.equals(car_id))
                is_new = false;
        }
        if (is_new)
            ed.putString(Names.CARS, preferences.getString(Names.CARS, "") + "," + car_id);
        ed.commit();
        finish();
        Intent intent = new Intent(this, FetchService.class);
        intent.putExtra(Names.ID, car_id);
        startService(intent);
        if (is_new && (spCars.getVisibility() == View.VISIBLE)) {
            Intent setup = new Intent(this, CarPreferences.class);
            setup.putExtra(Names.ID, car_id);
            startActivity(setup);
        }
    }

    void showError(String text) {
        String message = getString(R.string.key_error);
        if (text != null) {
            if (text.equals("Security Service Error")) {
                message = getString(R.string.invalid_key);
            } else {
                message += " " + text;
            }
        }
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
        if (dlgCheck != null)
            dlgCheck.dismiss();
        dlgCheck = null;
    }

    void showProgress() {
        dlgCheck = new ProgressDialog(this);
        dlgCheck.setMessage(getString(R.string.check_api));
        dlgCheck.show();
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
