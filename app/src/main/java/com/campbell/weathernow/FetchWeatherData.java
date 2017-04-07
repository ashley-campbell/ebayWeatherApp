package com.campbell.weathernow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class FetchWeatherData {

    private static final String TAG = FetchWeatherData.class.getSimpleName();
    public String apiKey = "67868b38cc81592f154f0c4172624a20";

    public FetchWeatherData() {
    }

    public String makeAPICall(String apiUrl) {
        String response = null;
        try {
            //Make request
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("x-api-key", apiKey);

            //Read response
            InputStream in = new BufferedInputStream(connection.getInputStream());
            response = makeString(in);
            connection.disconnect();
            in.close();

        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (ProtocolException e) {
            Log.e(TAG, "ProtocolException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
        return response;
    }

    public String makeString(InputStream in) {
        //Setup reader to convert Stream to String
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String input;

        try {
            while ((input = reader.readLine()) != null) {
                sb.append(input).append('\n');
            }
            reader.close();
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public Bitmap getWeatherIcon(String iconCode){
        Bitmap weatherIcon = null;

        if(iconCode != null) {
            try {
                String iconURL = "http://openweathermap.org/img/w/" + iconCode + ".png";

                //Stream and Response
                InputStream in = new java.net.URL(iconURL).openStream();
                weatherIcon = BitmapFactory.decodeStream(in);
                in.close();

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
        }

        return weatherIcon;
    }
}