package com.android.duongdb.weather.helpers;

import android.util.Log;

import com.android.duongdb.weather.models.ForecastWeather;
import com.android.duongdb.weather.models.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
public class DataHelper {
    public static Weather renderWeather(String data) throws JSONException {
        JSONObject json = new JSONObject(data);
        Weather weather = new Weather();
        try {
            String[] s = new String[4];

            JSONObject query = json.getJSONObject("query");
            JSONObject results = query.getJSONObject("results");
            JSONObject channel = results.getJSONObject("channel");
            JSONObject location = channel.getJSONObject("location");
            JSONObject item = channel.getJSONObject("item");
            JSONObject condition = item.getJSONObject("condition");
            JSONObject wind = channel.getJSONObject("wind");
            JSONObject atmosphere = channel.getJSONObject("atmosphere");
            JSONObject astronomy = channel.getJSONObject("astronomy");

            weather.setCityName(location.getString("city"));
            weather.setCountryName(location.getString("country"));
            String[] splited = channel.getString("lastBuildDate").split(" ");
            weather.setUpdateTime(splited[0]+ " " + splited[4] +" "+ splited[5]);
            weather.setTemp((int) ((condition.getDouble("temp") - 32) / 1.8));
            weather.setCondCode(condition.getInt("code"));
            weather.setCondText(condition.getString("text"));
            weather.setWindSpeed(wind.getInt("speed"));
            weather.setWindDirection(mapWindName(wind.getInt("direction")));
            weather.setHumidity(atmosphere.getInt("humidity"));
            weather.setPressure(atmosphere.getDouble("pressure"));
            weather.setVisibility(atmosphere.getDouble("visibility"));
            weather.setSunrise(astronomy.getString("sunrise"));
            weather.setSunset(astronomy.getString("sunset"));

            JSONArray forecastArray = item.getJSONArray("forecast");
            ArrayList<ForecastWeather> forecastWeathers = new ArrayList<>();
            for (int i=0; i < forecastArray.length(); i++) {
                ForecastWeather forecastWeather = new ForecastWeather();
                JSONObject forecastObject = forecastArray.getJSONObject(i);
                forecastWeather.setDay(forecastObject.getString("day"));
                forecastWeather.setLow((int) ((forecastObject.getDouble("low") - 32) / 1.8));
                forecastWeather.setHigh((int) ((forecastObject.getDouble("high") - 32) / 1.8));
                forecastWeather.setCode(forecastObject.getInt("code"));
                forecastWeathers.add(forecastWeather);
            }
            weather.setForecastWeathers(forecastWeathers);
        } catch (Exception e) {
            Log.e("WeatherApp", "Data not found");
            return null;
        }
        return weather;
    }

    public static String mapWindName(int direction){
        if (direction > 348.75 || direction <= 11.25) return "N";
        else if (direction <= 33.75) return "NNE";
        else if (direction <= 56.25) return "NE";
        else if (direction <= 78.75) return "ENE";
        else if (direction <= 101.25) return "E";
        else if (direction <= 123.75) return "ESE";
        else if (direction <= 146.25) return "SE";
        else if (direction <= 168.75) return "SSE";
        else if (direction <= 191.25) return "S";
        else if (direction <= 213.75) return "SSW";
        else if (direction <= 236.25) return "SW";
        else if (direction <= 258.75) return "WSW";
        else if (direction <= 281.25) return "W";
        else if (direction <= 303.75) return "WNW";
        else if (direction <= 326.25) return "NW";
        else return "NNW";
    }
}
