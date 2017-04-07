package com.campbell.weathernow;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();

    public static final int PERMISSIONS_REQUEST_LOCATION = 0;
    LocationManager locationManager;
    String provider;
    String deniedString;
    String locationString;
    String longitude;
    String latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionsCheck();
    }

    //If permissions are denied, enter re-ask flow
    public void deniedPermissions(){
        final TextView deniedText = (TextView) findViewById(R.id.denied);
        deniedText.setVisibility(View.VISIBLE);
        deniedString = "Need to grant location permissions to view weather for your location. " +
                "Please click button below to ask again.";
        deniedText.setText(deniedString);

        final Button deniedButton = (Button) findViewById(R.id.denied_button);
        deniedButton.setVisibility(View.VISIBLE);
        deniedButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // rerun permissions
                deniedText.setVisibility(View.GONE);
                deniedButton.setVisibility(View.GONE);
                permissionsCheck();
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return connectivityManager.getActiveNetworkInfo() != null;
    }

    //Permissions need to happen to check location
    public void permissionsCheck() {
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        //Ask for location permissions if not granted or start weather fetch
        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST_LOCATION);
        } else {
            //If permissions have already been granted, start running weather
            startWeather();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    startWeather();
                } else {
                    // Permission Denied
                    deniedPermissions();
                }
                default:
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void startWeather (){

        if (!isNetworkConnected()){
            //check if device has network connection, cannot run without internet connection
            TextView deniedText = (TextView) findViewById(R.id.denied);
            deniedText.setVisibility(View.VISIBLE);
            deniedString = "It appears you have no network connection, please check network";
            deniedText.setText(deniedString);
            return;
        }

        //Get coordinates to view weather, could be placed in separate method
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);

        if (provider != null && !provider.equals("")) {
            //Double check permissions
            if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions();
            }

            //It is possible for this to be null which the app should catch and not crash.
            //We could be more robust if we added a location listener rather than relying on
            //LastKnownLocation which is generally unreliable if location is turned on/off
            //If App is displaying null location with services ON, view Google Maps or anything
            //else that might ping your gps location to get a stored LastKnownLocation.
            //For sake of "keeping the task simple" I'm leaving as is
            Location location = locationManager.getLastKnownLocation(provider);

            if(location != null) {
                longitude = String.valueOf(location.getLongitude());
                latitude = String.valueOf(location.getLatitude());

                //Location is a pass, move forward with async task.
                new GetCurrentWeather().execute();
                new GetWeatherForcast().execute();

            } else {
                //location services disabled OR no lastknownlocation
                TextView deniedText = (TextView) findViewById(R.id.denied);
                deniedText.setVisibility(View.VISIBLE);
                deniedString = "Your location is null, please check location services.";
                deniedText.setText(deniedString);
            }
        } else {
            //no provider, gps, etc
            TextView deniedText = (TextView) findViewById(R.id.denied);
            deniedText.setVisibility(View.VISIBLE);
            deniedString = "Your location provider is null, please check services";
            deniedText.setText(deniedString);
        }
    }

    //AsyncTask to make Current Weather call and handle parsing.
    private class GetCurrentWeather extends AsyncTask<Void, Void, Void> {

        String mainString;
        String desString;
        String tempString;
        String humidityString;
        String windString;
        String iconString;
        Bitmap weatherIcon;

        @Override
        protected Void doInBackground(Void... arg0) {
            FetchWeatherData weatherData = new FetchWeatherData();

            String urlString = "http://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=imperial";
            String jsonStr = weatherData.makeAPICall(urlString);

            //Parsing could probably be handled in separate class
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONArray weatherArr = jsonObj.getJSONArray("weather");

                    for (int i = 0; i < weatherArr.length(); i++){
                        mainString = weatherArr.getJSONObject(i).getString("main");
                        desString = "(" + weatherArr.getJSONObject(i).getString("description") + ")";
                        iconString = weatherArr.getJSONObject(i).getString("icon");
                    }

                    locationString = jsonObj.getString("name");
                    windString = "Wind Speed: " + jsonObj.getJSONObject("wind").getString("speed") + " m/h";
                    tempString = "Temperature: " + jsonObj.getJSONObject("main").getString("temp") + "°F";
                    humidityString = "Humidity: " + jsonObj.getJSONObject("main").getString("humidity") + "%";

                    //Could separate loading of icon in own task in order
                    //to not hold up screen loading
                    weatherIcon = weatherData.getWeatherIcon(iconString);

                } catch (final JSONException e) {
                    Log.e(TAG, "Parsing Error: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Problem getting JSON.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            final TextView currentText = (TextView) findViewById(R.id.current_string);
            currentText.setVisibility(View.VISIBLE);

            final Button refreshButton = (Button) findViewById(R.id.refresh_button);
            refreshButton.setVisibility(View.VISIBLE);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // refresh
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            });

            //Set Current Location
            TextView locationText = (TextView) findViewById(R.id.location);
            locationText.setText(locationString);

            //Set Current Icon
            ImageView mainIcon = (ImageView) findViewById(R.id.current_icon);
            mainIcon.setImageBitmap(weatherIcon);

            //Set Current Main Description
            TextView mainText = (TextView) findViewById(R.id.current_main);
            mainText.setText(mainString);

            //Set Current Description
            TextView desText = (TextView) findViewById(R.id.current_des);
            desText.setText(desString);

            //Set Current Temperature
            TextView tempText = (TextView) findViewById(R.id.current_temperature);
            tempText.setText(tempString);

            //Set Current Humidity
            TextView humidityText = (TextView) findViewById(R.id.current_humidity);
            humidityText.setText(humidityString);

            //Set Current Wind
            TextView windText = (TextView) findViewById(R.id.current_wind);
            windText.setText(windString);

        }

    }

    //AsyncTask to make Weather Forcast call and handle parsing.
    private class GetWeatherForcast extends AsyncTask<Void, Void, Void> {

        //Forcast holders
        ArrayList<String> mainArr = new ArrayList<>();
        ArrayList<String> tempArr = new ArrayList<>();
        ArrayList<String> desArr = new ArrayList<>();
        ArrayList<Bitmap> bitmapArr = new ArrayList<>();

        @Override
        protected Void doInBackground(Void... arg0) {
            FetchWeatherData weatherData = new FetchWeatherData();

            String urlString = "http://api.openweathermap.org/data/2.5/forecast?lat=" + latitude + "&lon=" + longitude + "&units=imperial";
            String jsonStr = weatherData.makeAPICall(urlString);

            //Parsing could probably be handled in separate class
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONArray overAllArr = jsonObj.getJSONArray("list");
                    JSONArray hourForcast = new JSONArray();
                    JSONArray weatherArray;
                    JSONObject soloHour;

                    String mainString;
                    String iconString;
                    String tempString;
                    String desString;
                    Bitmap weatherIcon;


                    //Pull 3, 3 hour forcasts
                    for (int i = 0; i < 5; i++){
                        JSONObject object = overAllArr.getJSONObject(i);
                        hourForcast.put(i, object);
                    }

                    for (int j = 0; j < hourForcast.length(); j++){
                        soloHour = hourForcast.getJSONObject(j);
                        weatherArray = soloHour.getJSONArray("weather");

                        for(int k = 0; k < weatherArray.length(); k++){
                            mainString = weatherArray.getJSONObject(k).getString("main");
                            iconString = weatherArray.getJSONObject(k).getString("icon");
                            desString = weatherArray.getJSONObject(k).getString("description");

                            mainArr.add(k, mainString);
                            desArr.add(k, "(" + desString + ")");

                            weatherIcon = weatherData.getWeatherIcon(iconString);
                            bitmapArr.add(k, weatherIcon);
                        }

                        tempString = soloHour.getJSONObject("main").getString("temp") + "°F";

                        tempArr.add(j, tempString);
                    }

                } catch (final JSONException e) {
                    Log.e(TAG, "Parsing Error: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Problem getting JSON.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            final TextView forcastedText = (TextView) findViewById(R.id.forcasted_string);
            forcastedText.setVisibility(View.VISIBLE);

            final HorizontalScrollView scroll = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
            scroll.setVisibility(View.VISIBLE);

            //Bring up forcast views - showing 5 time sets
            TextView main1 = (TextView) findViewById(R.id.forcast1_main);
            TextView temp1 = (TextView) findViewById(R.id.forcast1_temperature);
            TextView des1 = (TextView) findViewById(R.id.forcast1_des);
            ImageView icon1 = (ImageView) findViewById(R.id.forcast1_icon);

            TextView main2 = (TextView) findViewById(R.id.forcast2_main);
            TextView temp2 = (TextView) findViewById(R.id.forcast2_temperature);
            TextView des2 = (TextView) findViewById(R.id.forcast2_des);
            ImageView icon2 = (ImageView) findViewById(R.id.forcast2_icon);

            TextView main3 = (TextView) findViewById(R.id.forcast3_main);
            TextView temp3 = (TextView) findViewById(R.id.forcast3_temperature);
            TextView des3 = (TextView) findViewById(R.id.forcast3_des);
            ImageView icon3 = (ImageView) findViewById(R.id.forcast3_icon);

            TextView main4 = (TextView) findViewById(R.id.forcast4_main);
            TextView temp4 = (TextView) findViewById(R.id.forcast4_temperature);
            TextView des4 = (TextView) findViewById(R.id.forcast4_des);
            ImageView icon4 = (ImageView) findViewById(R.id.forcast4_icon);

            TextView main5 = (TextView) findViewById(R.id.forcast5_main);
            TextView temp5 = (TextView) findViewById(R.id.forcast5_temperature);
            TextView des5 = (TextView) findViewById(R.id.forcast5_des);
            ImageView icon5 = (ImageView) findViewById(R.id.forcast5_icon);

            //Set Icon
            if(!bitmapArr.isEmpty() && bitmapArr.size() == 5) {
                icon1.setImageBitmap(bitmapArr.get(0));
                icon2.setImageBitmap(bitmapArr.get(1));
                icon3.setImageBitmap(bitmapArr.get(2));
                icon4.setImageBitmap(bitmapArr.get(3));
                icon5.setImageBitmap(bitmapArr.get(4));
            }

            //Set Temperature
            if(!tempArr.isEmpty() && tempArr.size() == 5) {
                temp1.setText(tempArr.get(0));
                temp2.setText(tempArr.get(1));
                temp3.setText(tempArr.get(2));
                temp4.setText(tempArr.get(3));
                temp5.setText(tempArr.get(4));
            }

            //Set Main Description
            if(!mainArr.isEmpty() && mainArr.size() == 5) {
                main1.setText(mainArr.get(0));
                main2.setText(mainArr.get(1));
                main3.setText(mainArr.get(2));
                main4.setText(mainArr.get(3));
                main5.setText(mainArr.get(4));
            }

            //Set Description
            if(!desArr.isEmpty() && desArr.size() == 5) {
                des1.setText(desArr.get(0));
                des2.setText(desArr.get(1));
                des3.setText(desArr.get(2));
                des4.setText(desArr.get(3));
                des5.setText(desArr.get(4));
            }
        }
    }
}