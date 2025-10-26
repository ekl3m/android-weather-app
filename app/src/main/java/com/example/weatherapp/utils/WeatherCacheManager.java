package com.example.weatherapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherCacheManager {

    private static final String PREFS_NAME = "weather_cache";
    private static final String KEY_ACTIVE_CITY = "active_city";
    private static final String KEY_ACTIVE_CITY_TIMESTAMP = "active_city_ts";
    private static final String KEY_CITY_CARD = "city_card_"; // + city
    private static final String KEY_CITY_CARD_TIMESTAMP = "city_card_ts_"; // + city

    // Zapisuje pełne dane (dla aktywnego miasta)
    public static void saveActiveCityWeatherCache(Context context, JSONObject weather, JSONObject daily, JSONObject hourly, JSONObject airQuality) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject combined = new JSONObject();
        try {
            combined.put("weather", weather);
            combined.put("daily", daily);
            combined.put("hourly", hourly);
            combined.put("air_quality", airQuality);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("WEATHER_CACHE", "Zapisuję dane cache aktywnego miasta: " + combined.toString());

        prefs.edit()
                .putString(KEY_ACTIVE_CITY, combined.toString())
                .putLong(KEY_ACTIVE_CITY_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    // Wczytuje pełne dane (dla aktywnego miasta)
    public static JSONObject loadActiveCityWeatherCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ACTIVE_CITY, null);
        Log.d("WEATHER_CACHE", "Wczytuję dane z cache: " + json);
        if (json == null) return null;

        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long getActiveCityWeatherTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_ACTIVE_CITY_TIMESTAMP, 0);
    }

    // Zapisuje tylko skrócone dane (/weather + z /forecast/daily samo min max temp) dla zapisanych miast
    public static void saveCityWeatherCardCache(Context context, String cityName, JSONObject currentWeather, JSONObject dailyForecast) {
        JSONObject combined = new JSONObject();
        try {
            combined.put("weather", currentWeather);
            combined.put("daily", dailyForecast);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        Log.d("WEATHER_CACHE", "Zapisuję cache i timestamp dla: " + cityName);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CITY_CARD + cityName.trim(), combined.toString())
                .putLong(KEY_CITY_CARD_TIMESTAMP + cityName.trim(), System.currentTimeMillis())
                .apply();
    }

    // Wczytuje skrócone dane
    public static JSONObject loadCityWeatherCardCache(Context context, String cityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CITY_CARD + cityName.trim(), null);
        if (json == null) return null;

        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long getCityWeatherCardTimestamp(Context context, String cityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Log.d("WEATHER_CACHE", "Pobrano timestamp dla " + cityName + ": " + prefs.getLong(KEY_CITY_CARD_TIMESTAMP + cityName.trim(), 0));

        return prefs.getLong(KEY_CITY_CARD_TIMESTAMP + cityName.trim(), 0);
    }
}