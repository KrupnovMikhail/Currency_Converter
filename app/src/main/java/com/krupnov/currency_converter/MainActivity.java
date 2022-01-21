package com.krupnov.currency_converter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DownloadJSONTask task = new DownloadJSONTask();
        task.execute("https://www.cbr-xml-daily.ru/daily_json.js");


    }

    private class DownloadJSONTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
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
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("Result", s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                JSONObject valute = jsonObject.getJSONObject("Valute");
                ArrayList<JSONObject> list = new ArrayList<>();
                ArrayList<String> data = new ArrayList<>();
                Iterator<String> my = valute.keys();
                while (my.hasNext()){
                    String a = my.next();
//                    Log.i("DATE54", my.next());
                    list.add(valute.getJSONObject(a));
                    data.add(a);

                }
                Log.i("DATA10", data.get(0));
                ArrayAdapter<String> listadapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, data);
                listView = (ListView) findViewById(R.id.listView);
                listView.setAdapter(listadapter);
                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setText(data.get(0));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}