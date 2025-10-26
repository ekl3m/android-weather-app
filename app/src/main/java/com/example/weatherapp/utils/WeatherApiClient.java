package com.example.weatherapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherApiClient {

    private static final String API_KEY = "afa9a2f771bcd268f6eca812f085ae36";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    public interface WeatherCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface CoordinatesListCallback {
        void onSuccess(org.json.JSONArray cities);
        void onError(String error);
    }

    public static void getWeather(Context context, String cityName, String units, final WeatherCallback callback) {
        String url = BASE_URL + "?q=" + cityName + "&appid=" + API_KEY + "&units=" + units + "&lang=pl";

        Log.d("WEATHER_API", "Żądanie URL (weather): " + url);

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                callback.onSuccess(response);
            },
            error -> {
                callback.onError(error.toString());
            }
        );

        queue.add(jsonObjectRequest);
    }

    public static void getHourlyForecast(Context context, String cityName, String units, final WeatherCallback callback) {
        String url = "https://api.openweathermap.org/data/2.5/forecast/hourly?q=" + cityName + "&appid=" + API_KEY + "&units=" + units + "&lang=pl";

        Log.d("WEATHER_API", "Żądanie URL (hourly): " + url);

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                callback.onSuccess(response);
            },
            error -> {
                callback.onError(error.toString());
            }
        );

        queue.add(jsonObjectRequest);
    }

    public static void getDailyForecast(Context context, String cityName, String units, final WeatherCallback callback) {
        String url = "https://api.openweathermap.org/data/2.5/forecast/daily?q=" + cityName + "&appid=" + API_KEY + "&units=" + units + "&lang=pl" + "&cnt=14"; // 14 dni

        Log.d("WEATHER_API", "Żądanie URL (daily): " + url);

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                callback.onSuccess(response);
            },
            error -> {
                callback.onError(error.toString());
            }
        );

        queue.add(jsonObjectRequest);
    }

    public static void getAirQuality(Context context, double lat, double lon, final WeatherCallback callback) {
        String url = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                callback.onSuccess(response);
            },
            error -> {
                callback.onError(error.toString());
            }
        );

        queue.add(request);
    }

    public static void getCoordinates(Context context, String cityName, final WeatherCallback callback) {
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&limit=1&appid=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                if (response.length() > 0) {
                    callback.onSuccess(response.optJSONObject(0));
                } else {
                    callback.onError("Brak danych geolokalizacji.");
                }
            },
            error -> {
                callback.onError(error.toString());
            }
        );

        queue.add(request);
    }

    public static void getCitySuggestions(Context context, String cityName, final CoordinatesListCallback callback) {
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&limit=5&appid=" + API_KEY + "&lang=pl";

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() > 0) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError("Brak wyników geolokalizacji.");
                    }
                },
                error -> {
                    callback.onError(error.toString());
                }
        );

        queue.add(request);
    }

    public static void fetchWeatherDataForSavedLocations(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("locations_prefs", Context.MODE_PRIVATE);
        String savedList = prefs.getString("saved_locations", "[]");

        // Jeśli nie ma zapisanych lokalizacji, dodaj domyślną Łódź
        if (savedList == null || savedList.equals("[]")) {
            JSONArray defaultArray = new JSONArray();
            JSONObject defaultCity = new JSONObject();
            try {
                defaultCity.put("query", "Lodz");
                defaultCity.put("name", "Łódź");
                defaultArray.put(defaultCity);
                prefs.edit().putString("saved_locations", defaultArray.toString()).apply();
                savedList = defaultArray.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (!prefs.contains("active_city")) {
            prefs.edit().putString("active_city", "Lodz").apply();
        }

        Log.d("WEATHER_API", "Zapisane miasta: " + savedList);

        try {
            JSONArray array = new JSONArray(savedList); // Tablica obiektów z query + name

            // Pobierz dane pogodowe dla aktywnego miasta
            String activeCity = prefs.getString("active_city", "Lodz");

            Log.d("WEATHER_API", "Aktywne miasto: " + activeCity);

            getWeather(context, activeCity, "metric", new WeatherCallback() {
                @Override
                public void onSuccess(JSONObject weatherResponse) {
                    getDailyForecast(context, activeCity, "metric", new WeatherCallback() {
                        @Override
                        public void onSuccess(JSONObject dailyResponse) {
                            getHourlyForecast(context, activeCity, "metric", new WeatherCallback() {
                                @Override
                                public void onSuccess(JSONObject hourlyResponse) {
                                    getCoordinates(context, activeCity, new WeatherCallback() {
                                        @Override
                                        public void onSuccess(JSONObject coordResponse) {
                                            try {
                                                double lat = coordResponse.getDouble("lat");
                                                double lon = coordResponse.getDouble("lon");

                                                getAirQuality(context, lat, lon, new WeatherCallback() {
                                                    @Override
                                                    public void onSuccess(JSONObject airResponse) {
                                                        Log.d("WEATHER_API", "Zapisuję pełny cache dla aktywnego miasta: " + activeCity);
                                                        WeatherCacheManager.saveActiveCityWeatherCache(
                                                                context,
                                                                weatherResponse,
                                                                dailyResponse,
                                                                hourlyResponse,
                                                                airResponse
                                                        );
                                                    }

                                                    @Override
                                                    public void onError(String error) {
                                                        Log.e("WEATHER_API", "Błąd AQI: " + error);
                                                    }
                                                });
                                            } catch (JSONException e) {
                                                Log.e("WEATHER_API", "Błąd parsowania koordynatów: " + e.getMessage());
                                            }
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.e("WEATHER_API", "Błąd pobierania koordynatów: " + error);
                                        }
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("WEATHER_API", "Błąd hourly forecast: " + error);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("WEATHER_API", "Błąd daily forecast: " + error);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("WEATHER_API", "Błąd danych pogodowych: " + error);
                }
            });

            // Pobierz dane pogodowe dla zapisanych miast
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String cityQuery = obj.getString("query");

                getWeather(context, cityQuery, "metric", new WeatherCallback() {
                    @Override
                    public void onSuccess(JSONObject weatherResponse) {
                        getDailyForecast(context, cityQuery, "metric", new WeatherCallback() {
                            @Override
                            public void onSuccess(JSONObject dailyResponse) {
                                Log.d("WEATHER_API", "Zapisuję card cache dla miasta: " + cityQuery);
                                WeatherCacheManager.saveCityWeatherCardCache(context, cityQuery, weatherResponse, dailyResponse);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("WEATHER_API", "Błąd forecastu dla " + cityQuery + ": " + error);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("WEATHER_API", "Błąd danych dla " + cityQuery + ": " + error);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}