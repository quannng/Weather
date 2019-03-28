package com.android.duongdb.weather.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.duongdb.weather.R;
import com.android.duongdb.weather.adapters.WeatherRecyclerAdapter;
import com.android.duongdb.weather.databases.DatabaseHelper;
import com.android.duongdb.weather.databases.UpdateCityThread;
import com.android.duongdb.weather.dialogs.SearchDialog;
import com.android.duongdb.weather.helpers.DataHelper;
import com.android.duongdb.weather.helpers.DialogHelper;
import com.android.duongdb.weather.helpers.ImageHelper;
import com.android.duongdb.weather.helpers.PreferenceHelper;
import com.android.duongdb.weather.models.HistoryModel;
import com.android.duongdb.weather.models.Weather;
import com.android.duongdb.weather.models.WeatherModel;
import com.android.duongdb.weather.utils.Constants;
import com.android.duongdb.weather.utils.DataLoadListener;
import com.android.duongdb.weather.utils.DatabaseUpdateListener;
import com.android.duongdb.weather.utils.JsonDataLoader;
import com.android.duongdb.weather.view.RevealLayout;

import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DataLoadListener, LocationListener {

    private RecyclerView weatherRecycler;
    private WeatherRecyclerAdapter weatherAdapter;
    private ArrayList<WeatherModel> weatherModels;
    private ImageButton refreshButton, searchButton, historyButton;
    private SearchDialog searchDialog;
    private WeatherModel weatherModel;
    private PreferenceHelper preferences;
    private JsonDataLoader dataLoader;
    private Animation rotateAnimation, fadeInAnimation;
    private LocationManager locationManager;
    private DatabaseHelper databaseHelper;

    private RevealLayout revealLayout;

    private View line;
    private TextView cityText, nowTempText, minTempText, maxTempText, weatherText, dataText;
    private ImageView weatherIcon, background, detailBackground, detailIcon;
    private TextView detailCity, detailTemp, detailTime, detailWeather, detailTemperature, detailAstronomy, detailAtmosphere, detailWindy;

    private static final int HISTORY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = PreferenceHelper.getInstance(this);

        cityText = (TextView) findViewById(R.id.city_name);
        nowTempText = (TextView) findViewById(R.id.now_temp);
        minTempText = (TextView) findViewById(R.id.min_temp);
        maxTempText = (TextView) findViewById(R.id.max_temp);
        weatherText = (TextView) findViewById(R.id.weather_name);
        dataText = (TextView) findViewById(R.id.data_text);

        detailCity = (TextView) findViewById(R.id.detail_city);
        detailTemp = (TextView) findViewById(R.id.detail_temp);
        detailTime = (TextView) findViewById(R.id.detail_time);
        detailWeather = (TextView) findViewById(R.id.detail_weather_name);
        detailTemperature = (TextView) findViewById(R.id.detail_temperature);
        detailAstronomy = (TextView) findViewById(R.id.detail_astronomy);
        detailAtmosphere = (TextView) findViewById(R.id.detail_atmosphere);
        detailWindy = (TextView) findViewById(R.id.detail_windy);

        weatherIcon = (ImageView) findViewById(R.id.weather_icon);
        background = (ImageView) findViewById(R.id.background);
        detailBackground = (ImageView) findViewById(R.id.background_detail);
        detailIcon = (ImageView) findViewById(R.id.detail_weather_icon);

        line = findViewById(R.id.main_line);

        revealLayout = (RevealLayout) findViewById(R.id.reveal_layout);

        refreshButton = (ImageButton) findViewById(R.id.refresh_button);
        searchButton = (ImageButton) findViewById(R.id.search_button);
        historyButton = (ImageButton) findViewById(R.id.history_button);
        weatherRecycler = (RecyclerView) findViewById(R.id.forecast_recycler);
        weatherRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        weatherModels = new ArrayList<>();

        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        databaseHelper = new DatabaseHelper(getApplicationContext());

        initRevealLayout();
        tryGetDataOffline();
        tryToGetDataOnline();


        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToGetDataOnline();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchDialog = new SearchDialog(MainActivity.this, MainActivity.this);
                searchDialog.show();
                if (dataLoader != null) dataLoader.cancel(true);
                if (locationManager != null) locationManager.removeUpdates(MainActivity.this);
                refreshButton.clearAnimation();
            }
        });

        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, HistoryActivity.class), HISTORY_REQUEST);
                if (dataLoader != null) dataLoader.cancel(true);
                if (locationManager != null) locationManager.removeUpdates(MainActivity.this);
                refreshButton.clearAnimation();
            }
        });
    }

    private void initRevealLayout() {
        revealLayout.setOnClickListener(null);
        revealLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    Log.d("SingleChildActivity", "x: " + event.getX() + ", y: " + event.getY());
                    revealLayout.next((int) event.getX(), (int) event.getY());
                    return true;
                }
                return false;
            }
        });
    }

    private void tryGetDataOffline() {
        String jsonData = preferences.getString("DATA", "");
        if (!jsonData.equals("")) {
            try {
                setData(DataHelper.renderWeather(jsonData));
            } catch (JSONException e) {
                Toast.makeText(this, "Cant get data. Please check and try again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addWeatherToDatabase(Weather weather) {
        HistoryModel historyModel = new HistoryModel();
        historyModel.setCity(weather.getCityName());
        historyModel.setDate(weather.getUpdateTime());
        historyModel.setCencius(weather.getTemp() + "°C");
        historyModel.setName(weather.getCondText());
        historyModel.setImageId(weather.getCondCode());

        UpdateCityThread updateCityThread = new UpdateCityThread(this);
        updateCityThread.execute(historyModel);
        updateCityThread.setDatabaseUpdateListener(new DatabaseUpdateListener() {
            @Override
            public void onFinish(boolean result) {
                if (!result)
                    Toast.makeText(MainActivity.this, "Cant add city to history!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "Success add city to history!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setData(Weather weather) {
        String[] date = weather.getUpdateTime().split(",");
        cityText.setText(weather.getCityName() + ", " + weather.getCountryName());
        preferences.putString("CITY", weather.getCityName());
        nowTempText.setText(weather.getTemp() + "°C");
        minTempText.setText(weather.getForecastWeathers().get(0).getLow() + "°C");
        maxTempText.setText(weather.getForecastWeathers().get(0).getHigh() + "°C");
        weatherText.setText(weather.getCondText());
        int drawableId = ImageHelper.getBackgroundIdFromCode(weather.getCondCode());

        weatherIcon.setImageResource(ImageHelper.getIconIdFromCode(weather.getCondCode()));
        weatherIcon.startAnimation(fadeInAnimation);

        background.setImageResource(drawableId);
        background.startAnimation(fadeInAnimation);

        weatherModels.clear();
        line.setVisibility(View.VISIBLE);
        dataText.setVisibility(View.GONE);
        for (int i = 0; i < weather.getForecastWeathers().size() - 1; i++) {
            weatherModel = new WeatherModel(weather.getForecastWeathers().get(i + 1).getDay(),
                    ImageHelper.getIconIdFromCode(weather.getForecastWeathers().get(i + 1).getCode()),
                    weather.getForecastWeathers().get(i + 1).getLow() + "/" +
                            weather.getForecastWeathers().get(i + 1).getHigh());
            weatherModels.add(weatherModel);
        }
        weatherAdapter = new WeatherRecyclerAdapter(getApplicationContext(), weatherModels);
        weatherRecycler.setAdapter(weatherAdapter);

        detailCity.setText(weather.getCityName());
        detailTemp.setText(weather.getTemp() + "°C");
        detailTime.setText(date[1]);
        detailWeather.setText(weather.getCondText());
        detailTemperature.setText("Temperature\nLow: " + weather.getForecastWeathers().get(0).getLow() + "°C\t\tHigh: " + weather.getForecastWeathers().get(0).getHigh() + "°C");
        detailAstronomy.setText("Astronomy\nSunset: " + weather.getSunset() + "\t\tSunrise: " + weather.getSunrise());
        detailAtmosphere.setText("Atmosphere\nHumidity: " + weather.getHumidity() + "%\t\tVisibility: " + weather.getVisibility() + "km\t\tPressure: " + weather.getPressure() + "mb");
        detailWindy.setText("Windy\nSpeech: " + weather.getWindSpeed() + "km/h\t\tDirection: " + weather.getWindDirection());
        detailBackground.setImageResource(ImageHelper.getBackgroundIdNextFromCode(weather.getCondCode(), drawableId));
        detailIcon.setImageResource(ImageHelper.getIconIdFromCode(weather.getCondCode()));

        addWeatherToDatabase(weather);
    }

    @Override
    public void onFinish(Weather weather, boolean isLoctionSet) {
        refreshButton.clearAnimation();
        if (weather != null)
            setData(weather);
        else
            Toast.makeText(getBaseContext(), "Cant get data. Please check and try again", Toast.LENGTH_SHORT).show();
    }

    public void queryWeather(String address, boolean isLocationSet) {
        refreshButton.startAnimation(rotateAnimation);
        dataLoader = new JsonDataLoader(this, isLocationSet, this);
        dataLoader.execute(address);
    }

    public void tryToGetDataOnline() {
        String cityData = preferences.getString("CITY", "");

        if (!DialogHelper.isNetworkConnected(getBaseContext()))
            DialogHelper.showNetworkDialog(MainActivity.this);
        else if (cityData.equals("")) {
            Toast.makeText(MainActivity.this, "Your city not avaiable, using location!", Toast.LENGTH_SHORT).show();
            if (DialogHelper.getGPSStatus(this) != null) {
                refreshButton.startAnimation(rotateAnimation);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Location location = locationManager.getLastKnownLocation(DialogHelper.getGPSStatus(this));
                if(location != null) onLocationChanged(location);
                else locationManager.requestLocationUpdates(DialogHelper.getGPSStatus(this), 60*1000, 10, this);
            }
            else DialogHelper.showLocationDialog(MainActivity.this);
        }
        else queryWeather(cityData, false);

    }
    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            queryWeather("(" + location.getLatitude()+ "," + location.getLongitude() + ")", false);
        }
        else Toast.makeText(getBaseContext(), "Cant get data. Please check and try again", Toast.LENGTH_SHORT).show();
        locationManager.removeUpdates(this);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HISTORY_REQUEST && resultCode == RESULT_OK) {
            String cityName = data.getStringExtra(Constants.CITY);
            queryWeather(cityName, false);
        }
    }
}
