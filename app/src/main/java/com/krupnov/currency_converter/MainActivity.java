package com.krupnov.currency_converter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private JSONObject valute;
    private ArrayList<JSONObject> list;
    private ArrayList<String> listValuteName;
    private String jsonString;
    private String textViewString;
    private String editTextString;

    private int cellNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        cellNumber = preferences.getInt("cellNumber", -1);
        jsonString = preferences.getString("JSONString", "");
        textViewString = preferences.getString("textViewString", "");
        editTextString = preferences.getString("editTextString", "");
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        DownloadJSONTask task = new DownloadJSONTask();
        task.execute("https://www.cbr-xml-daily.ru/daily_json.js");

        TextView textView = (TextView) findViewById(R.id.textView);
        ListView listView = (ListView) findViewById(R.id.listView);
        EditText editText = (EditText) findViewById(R.id.editText);
        Button button = (Button) findViewById(R.id.button);
        editText.setText(editTextString);
        textView.setText(textViewString);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                JSONObject elementJSON = list.get(i);
                try {
                    cellNumber = i;
                    preferences.edit().putInt("cellNumber", cellNumber).apply();
                    double number = Double.parseDouble(editText.getText().toString());
                    double nominal = Double.parseDouble(elementJSON.getString("Nominal"));
                    double value = Double.parseDouble(elementJSON.getString("Value"));
                    double result = number/(value/nominal);
                    BigDecimal resultEnd = new BigDecimal(result);
                    resultEnd = resultEnd.setScale(4, RoundingMode.DOWN);
                    textView.setText(listValuteName.get(i) + ": " + resultEnd.toString());
                    preferences.edit().putString("textViewString", listValuteName.get(i) + ": " + resultEnd.toString()).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    textView.setText("Вы ввели неправильно число");
                    preferences.edit().putString("textViewString", "Вы ввели неправильно число").apply();
                }
            }
        });


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                editTextString = editable.toString();
                preferences.edit().putString("editTextString", editTextString).apply();
                try {
                    if (cellNumber >= 0) {
                        listView.performItemClick(listView.getAdapter().getView(cellNumber, null, null),
                                cellNumber,
                                listView.getAdapter().getItemId(cellNumber));
                    }
                } catch (NullPointerException e) {

                }

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jsonString = "";
                textViewString = "";
                editTextString = "";
                cellNumber = -1;
                preferences.edit().putString("JSONString", jsonString).apply();
                preferences.edit().putString("textViewString", textViewString).apply();
                preferences.edit().putString("editTextString", editTextString).apply();
                preferences.edit().putInt("cellNumber", cellNumber).apply();
                editText.setText(editTextString);
                textView.setText(textViewString);


                DownloadJSONTask task = new DownloadJSONTask();
                task.execute("https://www.cbr-xml-daily.ru/daily_json.js");

            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                button.callOnClick();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.callOnClick();
                    }
                });
            }
        }, 0,1000 * 3600);


    }

    private class DownloadJSONTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            if (jsonString.equals("")) {
                URL url = null;
                HttpURLConnection urlConnection = null;
                StringBuilder result = new StringBuilder();
                try {
                    url = new URL(strings[0]);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line = reader.readLine();
                    while (line != null) {
                        result.append(line.trim());
                        line = reader.readLine();
                    }
                    return result.toString();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            } else {
                return jsonString;
            }
            return  null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            jsonString = s;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            preferences.edit().putString("JSONString", jsonString).apply();
            try {
                JSONObject jsonObject = new JSONObject(s);
                valute = jsonObject.getJSONObject("Valute");
                list = new ArrayList<>();
                listValuteName = new ArrayList<>();
                ArrayList<String> data = new ArrayList<>();
                Iterator<String> my = valute.keys();
                while (my.hasNext()){
                    String a = my.next();
                    listValuteName.add(a);
                    JSONObject itemJSON = valute.getJSONObject(a);
                    list.add(valute.getJSONObject(a));
                    double value = Double.parseDouble(itemJSON.getString("Value")) / Double.parseDouble(itemJSON.getString("Nominal"));
                    BigDecimal valueBig = new BigDecimal(value);
                    valueBig = valueBig.setScale(4, RoundingMode.DOWN);
                    data.add(a + ":    " + itemJSON.getString("Name") +"\n" +
                            "1 " + a + " = " + valueBig + " RUB");

                }

                ArrayAdapter<String> listadapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, data);
                listView = (ListView) findViewById(R.id.listView);
                listView.setAdapter(listadapter);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}