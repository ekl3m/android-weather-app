package com.example.weatherapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.weatherapp.MainActivity;
import com.example.weatherapp.R;
import com.example.weatherapp.models.DailyForecastItem;
import com.example.weatherapp.models.HourlyForecastItem;
import com.example.weatherapp.utils.UnitConverter;
import com.example.weatherapp.utils.WeatherApiClient;
import com.example.weatherapp.utils.WeatherCacheManager;
import com.example.weatherapp.utils.WeatherUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherFragment extends Fragment {

    private String cityName;
    private String units = "metric";
    private double currentForecastTemperature = 404;
    private int weatherCodeNow = 0;
    private View rootView;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public WeatherFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        prefs = requireContext().getSharedPreferences("locations_prefs", android.content.Context.MODE_PRIVATE);
        cityName = prefs.getString("active_city", "Lodz");
        prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        WeatherUtilities.testInternetWithWeatherCall(requireContext(),
            () -> {
                Log.e("WEATHER_FRAG", "Wchodzę do onOffline");
                JSONObject cached = WeatherCacheManager.loadActiveCityWeatherCache(requireContext());
                Log.e("WEATHER_FRAG", "Dane z cache: " + (cached != null ? cached.toString() : "null"));
                long ts = WeatherCacheManager.getActiveCityWeatherTimestamp(requireContext());
                if (cached != null) {
                    Log.e("WEATHER_FRAG", "Brak internetu – biorę z cache");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showConnectionBanner(ts, false, false);
                    }

                    requireActivity().runOnUiThread(() -> {
                        try {
                            updateFromCache(cached);
                        } catch (JSONException e) {
                            Log.e("WEATHER_FRAG", "Błąd parsowania cache: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e("WEATHER_FRAG", "Brak internetu i brak cache");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showConnectionBanner(ts, false, true);
                    }
                }
            },
            () -> {
                Log.e("WEATHER_FRAG", "Wchodzę do onOnline");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).hideConnectionBanner();
                }
                fetchAllWeatherData();
            }
        );
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAutoRefreshOnInDevSettings()) handler.post(refreshRunnable); // start pętli odświeżania
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable); // zatrzymaj pętlę
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            WeatherUtilities.testInternetWithWeatherCall(requireContext(),
                    () -> {
                        Log.e("WEATHER_FRAG", "Brak internetu – odświeżam z cache");
                        JSONObject cached = WeatherCacheManager.loadActiveCityWeatherCache(requireContext());
                        long ts = WeatherCacheManager.getActiveCityWeatherTimestamp(requireContext());

                        if (cached != null) {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).showConnectionBanner(ts, false, false);
                            }
                            try {
                                updateFromCache(cached);
                            } catch (JSONException e) {
                                Log.e("WEATHER_FRAG", "Błąd parsowania cache: " + e.getMessage());
                            }
                        } else {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).showConnectionBanner(ts, false, true);
                            }
                        }
                    },
                    () -> {
                        Log.e("WEATHER_FRAG", "Internet dostępny – pobieram z API");
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideConnectionBanner();
                        }
                        fetchAllWeatherData();
                    }
            );

            // Odpal ponownie za 30s
            handler.postDelayed(this, 30_000);
        }
    };

    public void fetchAllWeatherData() {
        WeatherApiClient.getWeather(getContext(), cityName, units, new WeatherApiClient.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded() || getActivity() == null) return;
                try {
                    String city = response.getString("name");
                    JSONObject main = response.getJSONObject("main");
                    double rawTemp = main.getDouble("temp");
                    currentForecastTemperature = rawTemp;

                    String tempUnit = prefs.getString("unit_temp", "celsius");
                    double temperature = tempUnit.equals("fahrenheit") ? (rawTemp * 9 / 5) + 32 : rawTemp;
                    String formattedTemp = String.format(Locale.getDefault(), "%.0f", temperature);

                    JSONArray weatherArray = response.getJSONArray("weather");
                    JSONObject weather = weatherArray.getJSONObject(0);
                    int weatherCode = weather.getInt("id");
                    weatherCodeNow = weatherCode;

                    requireActivity().runOnUiThread(() -> {
                        ((TextView) rootView.findViewById(R.id.text_city)).setText(city);
                        ((TextView) rootView.findViewById(R.id.text_temperature)).setText(formattedTemp);
                        ((TextView) rootView.findViewById(R.id.text_description)).setText(WeatherUtilities.getNiceDescriptionFromCode(weatherCode, WeatherUtilities.isNight(response)));
                    });

                    updateWindCard(response);
                    updateVisibilityCard(response);
                    updateHumidityCard(response);
                    updateAirPressure(response);
                    updateAirQuality(cityName);

                    WeatherUtilities.setBackgroundForWeatherCode(requireContext(), weatherCode, WeatherUtilities.isNight(response));
                } catch (JSONException e) {
                    Log.e("WEATHER_FRAG", "Błąd parsowania głównych danych: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("WEATHER_FRAG", "Błąd pobierania danych pogodowych: " + error);
            }
        });

        WeatherApiClient.getHourlyForecast(getContext(), cityName, units, new WeatherApiClient.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded() || getActivity() == null) return;
                try {
                    JSONArray hourlyList = response.getJSONArray("list");
                    List<HourlyForecastItem> forecast = new ArrayList<>();

                    for (int i = 1; i < Math.min(26, hourlyList.length()); i++) {
                        JSONObject hourData = hourlyList.getJSONObject(i);
                        double temp = hourData.getJSONObject("main").getDouble("temp");
                        int weatherCode = hourData.getJSONArray("weather").getJSONObject(0).getInt("id");
                        String dtText = hourData.getString("dt_txt");
                        String hour = dtText.substring(11, 13);
                        if (i == 1) {
                            hour = "Teraz";
                            temp = currentForecastTemperature;
                            weatherCode = weatherCodeNow;
                        }
                        String tempStr = String.format(Locale.getDefault(), "%.0f°", temp);
                        String iconCode = hourData.getJSONArray("weather").getJSONObject(0).getString("icon");
                        int iconResId = WeatherUtilities.getIconResId(weatherCode, iconCode);
                        forecast.add(new HourlyForecastItem(hour, tempStr, iconResId));
                    }

                    requireActivity().runOnUiThread(() -> populateHourlyForecast(forecast));
                } catch (JSONException e) {
                    Log.e("WEATHER_FRAG", "Błąd parsowania hourly: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("WEATHER_FRAG", "Błąd pobierania hourly: " + error);
            }
        });

        WeatherApiClient.getDailyForecast(getContext(), cityName, units, new WeatherApiClient.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded() || getActivity() == null) return;
                try {
                    JSONArray dailyList = response.getJSONArray("list");
                    List<DailyForecastItem> forecast = new ArrayList<>();
                    for (int i = 0; i < dailyList.length(); i++) {
                        JSONObject dayData = dailyList.getJSONObject(i);
                        JSONObject temp = dayData.getJSONObject("temp");
                        JSONObject weather = dayData.getJSONArray("weather").getJSONObject(0);
                        String dayLabel = (i == 0) ? "Dziś" : new java.text.SimpleDateFormat("EEE", new Locale("pl")).format(new java.util.Date(dayData.getLong("dt") * 1000));
                        String minStr = String.format(Locale.getDefault(), "%.0f°", temp.getDouble("min"));
                        String maxStr = String.format(Locale.getDefault(), "%.0f°", temp.getDouble("max"));
                        int iconResId = WeatherUtilities.getIconResId(weather.getInt("id"), weather.getString("icon"));
                        forecast.add(new DailyForecastItem(dayLabel, minStr, maxStr, iconResId));
                    }

                    requireActivity().runOnUiThread(() -> populateDailyForecast(forecast));
                } catch (JSONException e) {
                    Log.e("WEATHER_FRAG", "Błąd parsowania daily: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("WEATHER_FRAG", "Błąd pobierania daily: " + error);
            }
        });
    }

    // Metody populate/update
    private void populateHourlyForecast(List<HourlyForecastItem> items) {
        LinearLayout container = rootView.findViewById(R.id.hourly_container);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String tempUnit = prefs.getString("unit_temp", "celsius");

        for (HourlyForecastItem item : items) {
            View view = inflater.inflate(R.layout.item_hourly_forecast, container, false);
            ((TextView) view.findViewById(R.id.text_hour)).setText(item.getHour());
            ((TextView) view.findViewById(R.id.text_temp)).setText(UnitConverter.convertTemperature(WeatherUtilities.parseTemp(item.getTemperature()), tempUnit));            ((ImageView) view.findViewById(R.id.image_icon)).setImageResource(item.getIconResId());
            container.addView(view);
        }
    }

    private void populateDailyForecast(List<DailyForecastItem> items) {
        LinearLayout container = rootView.findViewById(R.id.daily_items_container);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        String tempUnit = prefs.getString("unit_temp", "celsius");

        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        for (DailyForecastItem item : items) {
            int min = WeatherUtilities.parseTemp(item.getMinTemp());
            int max = WeatherUtilities.parseTemp(item.getMaxTemp());
            globalMin = Math.min(globalMin, min);
            globalMax = Math.max(globalMax, max);
        }

        for (DailyForecastItem item : items) {
            View view = inflater.inflate(R.layout.item_daily_forecast, container, false);
            ((TextView) view.findViewById(R.id.text_day)).setText(item.getDay());

            int thisMin = WeatherUtilities.parseTemp(item.getMinTemp());
            int thisMax = WeatherUtilities.parseTemp(item.getMaxTemp());

            String minFormatted = UnitConverter.convertTemperature(thisMin, tempUnit);
            String maxFormatted = UnitConverter.convertTemperature(thisMax, tempUnit);

            ((TextView) view.findViewById(R.id.text_temp_min)).setText(minFormatted);
            ((TextView) view.findViewById(R.id.text_temp_max)).setText(maxFormatted);
            ((ImageView) view.findViewById(R.id.image_icon)).setImageResource(item.getIconResId());

            View bar = view.findViewById(R.id.temp_bar);
            View bg = view.findViewById(R.id.temp_bar_background);

            int finalGlobalMax = globalMax;
            int finalGlobalMin = globalMin;

            bg.post(() -> {
                int fullWidth = bg.getWidth();
                float range = finalGlobalMax - finalGlobalMin;
                float startRatio = (thisMin - finalGlobalMin) / range;
                float spanRatio = (thisMax - thisMin) / range;
                int marginStart = Math.round(startRatio * fullWidth);
                int barWidth = Math.round(spanRatio * fullWidth);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bar.getLayoutParams();
                params.width = barWidth;
                params.leftMargin = marginStart;
                bar.setLayoutParams(params);
            });

            container.addView(view);
        }
    }

    private void updateWindCard(JSONObject response) {
        try {
            JSONObject wind = response.getJSONObject("wind");

            double speed = wind.getDouble("speed");
            double gust = wind.has("gust") ? wind.getDouble("gust") : 0;
            int deg = wind.getInt("deg");

            String directionLabel = WeatherUtilities.getWindDirectionLabel(deg);
            String fullDirection = deg + "° " + directionLabel;

            String windUnit = prefs.getString("unit_wind", "mps");
            if (windUnit == null) windUnit = "mps";

            String speedText = UnitConverter.convertWind(speed, windUnit);
            String gustText = UnitConverter.convertWind(gust, windUnit);

            requireActivity().runOnUiThread(() -> {
                ((TextView) rootView.findViewById(R.id.text_wind_speed)).setText(speedText);
                ((TextView) rootView.findViewById(R.id.text_wind_gust)).setText(gustText);
                ((TextView) rootView.findViewById(R.id.text_wind_dir)).setText(fullDirection);

                ImageView arrow = rootView.findViewById(R.id.image_wind_arrow);
                arrow.setRotation(deg);
            });

        } catch (JSONException e) {
            Log.e("WIND_PARSE", "Błąd parsowania wiatru: " + e.getMessage());
        }
    }

    private void updateVisibilityCard(JSONObject response) {
        try {
            int visibilityMeters = response.getInt("visibility");

            String unit = prefs.getString("unit_visibility", "km");
            if (unit == null) unit = "km";

            String visibilityFormatted = UnitConverter.convertVisibility(visibilityMeters, unit);

            double visibilityKm = visibilityMeters / 1000.0;


            TextView range = rootView.findViewById(R.id.text_visibility_range);
            TextView desc = rootView.findViewById(R.id.text_visibility_description);

            range.setText(visibilityFormatted);
            desc.setText(WeatherUtilities.getVisibilityLabel((int) visibilityKm));
        } catch (JSONException e) {
            Log.e("VISIBILITY", "Błąd parsowania visibility: " + e.getMessage());
        }
    }

    private void updateHumidityCard(JSONObject response) {
        try {
            JSONObject main = response.getJSONObject("main");

            double temperature = main.getDouble("temp");       // w °C
            int humidity = main.getInt("humidity");            // w %

            // Oblicz punkt rosy (formuła Magnus-Tetens)
            double dewPoint = WeatherUtilities.calculateDewPoint(temperature, humidity);

            TextView percent = rootView.findViewById(R.id.text_humidity_percent);
            TextView dew = rootView.findViewById(R.id.text_dew_point);

            percent.setText(humidity + "%");
            String tempUnit = prefs.getString("unit_temp", "celsius");
            String dewPointFormatted = UnitConverter.convertTemperature(dewPoint, tempUnit);
            dew.setText("Punkt rosy w tej chwili wynosi " + dewPointFormatted + ".");

        } catch (JSONException e) {
            Log.e("HUMIDITY", "Błąd parsowania wilgotności: " + e.getMessage());
        }
    }

    private void updateAirQuality(String cityName) {
        WeatherApiClient.getCoordinates(requireContext(), cityName, new WeatherApiClient.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject location) {
                if (!isAdded() || getActivity() == null) return;
                try {
                    double lat = location.getDouble("lat");
                    double lon = location.getDouble("lon");

                    WeatherApiClient.getAirQuality(requireContext(), lat, lon, new WeatherApiClient.WeatherCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            if (!isAdded() || getActivity() == null) return;
                            try {
                                JSONArray list = response.getJSONArray("list");
                                JSONObject aqiObject = list.getJSONObject(0);
                                int aqi = aqiObject.getJSONObject("main").getInt("aqi");

                                requireActivity().runOnUiThread(() -> {
                                    TextView aqiText = rootView.findViewById(R.id.text_aqi_value);
                                    TextView descText = rootView.findViewById(R.id.text_aqi_description);
                                    View indicator = rootView.findViewById(R.id.aqi_indicator);
                                    View bar = ((ViewGroup) indicator.getParent()).getChildAt(0); // gradient

                                    aqiText.setText(String.valueOf(aqi));
                                    descText.setText(WeatherUtilities.getAQIDescription(aqi));

                                    bar.post(() -> {
                                        int barWidth = bar.getWidth();
                                        float ratio = Math.min(1f, (aqi - 1) / 4f); // AQI 1–5 → 0.0–1.0
                                        int indicatorPos = (int) (barWidth * ratio);
                                        indicator.setTranslationX(indicatorPos - indicator.getWidth() / 2f);
                                    });
                                });
                            } catch (JSONException e) {
                                Log.e("AIR_QUALITY", "Błąd parsowania AQI: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("AIR_QUALITY", "Błąd AQI: " + error);
                        }
                    });

                } catch (JSONException e) {
                    Log.e("GEO_API", "Błąd parsowania koordynatów: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("GEO_API", "Błąd geolokalizacji: " + error);
            }
        });
    }

    private void updateAirPressure(JSONObject response) {
        try {
            int pressure = response.getJSONObject("main").getInt("pressure"); // hPa

            requireActivity().runOnUiThread(() -> {
                TextView pressureText = rootView.findViewById(R.id.text_pressure_value);
                TextView unitText = rootView.findViewById(R.id.text_pressure_unit);
                String pressureUnit = prefs.getString("unit_pressure", "hPa");

                pressureText.setText(UnitConverter.convertPressure(pressure, pressureUnit));
                unitText.setText(UnitConverter.getPressureUnitLabel(pressureUnit));

                int minPressure = 980;
                int maxPressure = 1040;

                float angleRange = 180f;
                float ratio = (float)(pressure - minPressure) / (maxPressure - minPressure);
                ratio = Math.max(0f, Math.min(1f, ratio)); // bezpieczny zakres 0–1

                float rotation = -90f + (angleRange * ratio);

                FrameLayout arrow_container = rootView.findViewById(R.id.pressure_arrow_container);
                arrow_container.setRotation(rotation);
            });

        } catch (JSONException e) {
            Log.e("WEATHER_API", "Błąd parsowania ciśnienia: " + e.getMessage());
        }
    }

    public void setTargetCity(String newCityName) {
        cityName = newCityName;
    }

    private void updateFromCache(JSONObject data) throws JSONException {
        JSONObject weather = data.getJSONObject("weather");
        JSONObject daily = data.getJSONObject("daily");
        JSONObject hourly = data.getJSONObject("hourly");
        JSONObject airQuality = data.getJSONObject("air_quality");

        // Symuluj aktualizację danych pobierajac je z cache
        fetchAllWeatherDataFromCache(weather, daily, hourly, airQuality);
    }

    private void fetchAllWeatherDataFromCache(JSONObject weather, JSONObject daily, JSONObject hourly, JSONObject airQuality) {
        try {
            String city = weather.getString("name");
            JSONObject main = weather.getJSONObject("main");
            double rawTemp = main.getDouble("temp");
            currentForecastTemperature = rawTemp;

            String tempUnit = prefs.getString("unit_temp", "celsius");
            double temperature = tempUnit.equals("fahrenheit") ? (rawTemp * 9 / 5) + 32 : rawTemp;
            String formattedTemp = String.format(Locale.getDefault(), "%.0f", temperature);

            JSONArray weatherArray = weather.getJSONArray("weather");
            JSONObject weatherObj = weatherArray.getJSONObject(0);
            int weatherCode = weatherObj.getInt("id");

            ((TextView) rootView.findViewById(R.id.text_city)).setText(city);
            ((TextView) rootView.findViewById(R.id.text_temperature)).setText(formattedTemp);
            ((TextView) rootView.findViewById(R.id.text_description)).setText(WeatherUtilities.getNiceDescriptionFromCode(weatherCode, WeatherUtilities.isNight(weather)));

            updateWindCard(weather);
            updateVisibilityCard(weather);
            updateHumidityCard(weather);
            updateAirPressure(weather);
            updateAirQuality(city);

            WeatherUtilities.setBackgroundForWeatherCode(requireContext(), weatherCode, WeatherUtilities.isNight(weather));

            // hourly forecast
            JSONArray hourlyList = hourly.getJSONArray("list");
            List<HourlyForecastItem> forecast = new ArrayList<>();
            for (int i = 1; i < Math.min(26, hourlyList.length()); i++) {
                JSONObject hourData = hourlyList.getJSONObject(i);
                double temp = hourData.getJSONObject("main").getDouble("temp");
                int weatherCodeItem = hourData.getJSONArray("weather").getJSONObject(0).getInt("id");
                String dtText = hourData.getString("dt_txt");
                String hour = dtText.substring(11, 13);
                if (i == 1) {
                    hour = "Teraz";
                    temp = currentForecastTemperature;
                    weatherCodeItem = weatherCode;
                }
                String tempStr = String.format(Locale.getDefault(), "%.0f°", temp);
                String iconCode = hourData.getJSONArray("weather").getJSONObject(0).getString("icon");
                int iconResId = WeatherUtilities.getIconResId(weatherCodeItem, iconCode);
                forecast.add(new HourlyForecastItem(hour, tempStr, iconResId));
            }
            populateHourlyForecast(forecast);

            // daily forecast
            JSONArray dailyList = daily.getJSONArray("list");
            List<DailyForecastItem> dailyForecast = new ArrayList<>();
            for (int i = 0; i < dailyList.length(); i++) {
                JSONObject dayData = dailyList.getJSONObject(i);
                JSONObject temp = dayData.getJSONObject("temp");
                JSONObject weatherDaily = dayData.getJSONArray("weather").getJSONObject(0);
                String dayLabel = (i == 0) ? "Dziś" : new java.text.SimpleDateFormat("EEE", new Locale("pl")).format(new java.util.Date(dayData.getLong("dt") * 1000));
                String minStr = String.format(Locale.getDefault(), "%.0f°", temp.getDouble("min"));
                String maxStr = String.format(Locale.getDefault(), "%.0f°", temp.getDouble("max"));
                int iconResId = WeatherUtilities.getIconResId(weatherDaily.getInt("id"), weatherDaily.getString("icon"));
                dailyForecast.add(new DailyForecastItem(dayLabel, minStr, maxStr, iconResId));
            }
            populateDailyForecast(dailyForecast);

        } catch (JSONException e) {
            Log.e("WEATHER_FRAG", "Błąd parsowania danych cache: " + e.getMessage());
        }
    }

    private boolean isAutoRefreshOnInDevSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String refreshMode = prefs.getString("refresh_mode", "auto");
        return refreshMode.equals("auto");
    }
}

