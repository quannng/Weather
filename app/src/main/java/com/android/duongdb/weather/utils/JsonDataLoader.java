package com.android.duongdb.weather.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.duongdb.weather.helpers.PreferenceHelper;
import com.android.duongdb.weather.helpers.DataHelper;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class JsonDataLoader extends AsyncTask<String, Void, String> {

    private StringBuilder query;
    private DataLoadListener dataLoadListener;
    private Boolean isLocationSet;
    private PreferenceHelper preferences;
    private Context context;

    public JsonDataLoader(DataLoadListener dataLoadListener, Boolean isLocationSet, Context context) {
        this.isLocationSet = isLocationSet;
        this.context = context;
        this.query = new StringBuilder();
        query.append("https://query.yahooapis.com/v1/public/yql?q=");
        query.append("select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"");
        this.dataLoadListener = dataLoadListener;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected String doInBackground(String... params) {
        query.append(params[0]).append("\")&format=json");
        String address = query.toString().replace(" ", "%20");

        HttpURLConnection urlConnection = null;
        String result = "";
        Log.d("aaaaaaaaaa", address);
        try {
            URL url = new URL(address);
            urlConnection = (HttpURLConnection) url.openConnection();
            int code = urlConnection.getResponseCode();
            if(code==200){
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                if (in != null) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                    String line = "";

                    while ((line = bufferedReader.readLine()) != null)
                        result += line;
                }
                in.close();
            }

            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        finally {
            urlConnection.disconnect();
        }
        return result;

    }

    @Override
    protected void onPostExecute(String result) {
        try {
            dataLoadListener.onFinish(DataHelper.renderWeather(result), isLocationSet);
            preferences = PreferenceHelper.getInstance(context);
            preferences.putString("DATA", result);
        } catch (JSONException e) {
            dataLoadListener.onFinish(null, isLocationSet);
            e.printStackTrace();
        }
        super.onPostExecute(result);
    }



    public DataLoadListener getDataLoadListener() {
        return dataLoadListener;
    }

    public void setDataLoadListener(DataLoadListener dataLoadListener) {
        this.dataLoadListener = dataLoadListener;
    }
}