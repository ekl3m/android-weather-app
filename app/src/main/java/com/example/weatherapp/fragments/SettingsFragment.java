package com.example.weatherapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.weatherapp.MainActivity;
import com.example.weatherapp.R;
import com.example.weatherapp.utils.WeatherApiClient;
import com.example.weatherapp.utils.WeatherCacheManager;
import com.example.weatherapp.utils.WeatherUtilities;

import org.json.JSONObject;

public class SettingsFragment extends Fragment {

    private RadioGroup radioTemp, radioWind, radioPressure, radioVisibility, radioRefresh;
    private SwitchCompat devSettingsSwitch;
    private LinearLayout devSettingsContainer;
    private ImageButton refreshButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((MainActivity) requireActivity()).crossfadeBackground(
                ((MainActivity) requireActivity()).getLastWeatherBackgroundRes()
        );
        ((MainActivity) requireActivity()).changeConnectionBannerBg(false);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        radioTemp = view.findViewById(R.id.radio_temp);
        radioWind = view.findViewById(R.id.radio_wind);
        radioPressure = view.findViewById(R.id.radio_pressure);
        radioVisibility = view.findViewById(R.id.radio_visibility);
        devSettingsSwitch = view.findViewById(R.id.switch_dev_settings);
        devSettingsContainer = view.findViewById(R.id.dev_settings_container);
        radioRefresh = view.findViewById(R.id.radio_refresh);
        refreshButton = view.findViewById(R.id.button_refresh);

        radioTemp.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_temp_celsius) savePreference("unit_temp", "celsius");
            else if (checkedId == R.id.radio_temp_fahrenheit) savePreference("unit_temp", "fahrenheit");
        });

        radioWind.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_wind_ms) savePreference("unit_wind", "mps");
            else if (checkedId == R.id.radio_wind_kmh) savePreference("unit_wind", "kph");
            else if (checkedId == R.id.radio_wind_mph) savePreference("unit_wind", "mph");
        });

        radioPressure.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_pressure_hpa) savePreference("unit_pressure", "hpa");
            else if (checkedId == R.id.radio_pressure_mmHg) savePreference("unit_pressure", "mmHg");
        });

        radioVisibility.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_visibility_km) savePreference("unit_visibility", "km");
            else if (checkedId == R.id.radio_visibility_miles) savePreference("unit_visibility", "miles");
        });

        devSettingsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("dev_settings_enabled", Boolean.toString(isChecked));
            if (isChecked) {
                devSettingsContainer.setVisibility(View.VISIBLE);
            } else {
                devSettingsContainer.setVisibility(View.GONE);
            }
        });

        radioRefresh.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_refresh_auto) {
                savePreference("refresh_mode", "auto");
                refreshButton.setAlpha(0.4f);
                refreshButton.setEnabled(false);
            } else if (checkedId == R.id.radio_refresh_manual) {
                savePreference("refresh_mode", "manual");
                refreshButton.setAlpha(1.0f);
                refreshButton.setEnabled(true);
            }
        });

        refreshButton.setOnClickListener(v -> {
            if (!refreshButton.isEnabled()) return;

            refreshButton.animate()
                    .rotationBy(360f)
                    .setDuration(600)
                    .start();

            WeatherUtilities.testInternetWithWeatherCall(
                    requireContext(),
                    () -> { // offline
                        Log.e("DEV_SETTINGS", "Brak internetu – odświeżam z cache");
                        if (getActivity() instanceof MainActivity) {
                            long ts = WeatherCacheManager.getActiveCityWeatherTimestamp(requireContext());
                            JSONObject cached = WeatherCacheManager.loadActiveCityWeatherCache(requireContext());
                            if (cached != null) {
                                ((MainActivity) getActivity()).showConnectionBanner(ts, false, false);
                            } else {
                                ((MainActivity) getActivity()).showConnectionBanner(ts, false, true);
                            }
                        }
                    },
                    () -> { // online
                        Log.e("DEV_SETTINGS", "Mamy internet – odświeżam z API");
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideConnectionBanner();
                        }
                        WeatherApiClient.fetchWeatherDataForSavedLocations(requireContext());
                    }
            );
        });

        loadPreferences();
    }

    private void savePreference(String key, String value) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    private void loadPreferences() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        String tempUnit = prefs.getString("unit_temp", "celsius");
        if (tempUnit.equals("celsius")) radioTemp.check(R.id.radio_temp_celsius);
        else if (tempUnit.equals("fahrenheit")) radioTemp.check(R.id.radio_temp_fahrenheit);

        String windUnit = prefs.getString("unit_wind", "mps");
        if (windUnit.equals("mps")) radioWind.check(R.id.radio_wind_ms);
        else if (windUnit.equals("kph")) radioWind.check(R.id.radio_wind_kmh);
        else if (windUnit.equals("mph")) radioWind.check(R.id.radio_wind_mph);

        String pressureUnit = prefs.getString("unit_pressure", "hpa");
        if (pressureUnit.equals("hpa")) radioPressure.check(R.id.radio_pressure_hpa);
        else if (pressureUnit.equals("mmHg")) radioPressure.check(R.id.radio_pressure_mmHg);

        String visibilityUnit = prefs.getString("unit_visibility", "km");
        if (visibilityUnit.equals("km")) radioVisibility.check(R.id.radio_visibility_km);
        else if (visibilityUnit.equals("miles")) radioVisibility.check(R.id.radio_visibility_miles);

        boolean devSettingsEnabled = Boolean.parseBoolean(prefs.getString("dev_settings_enabled", "false"));
        devSettingsSwitch.setChecked(devSettingsEnabled);
        if (devSettingsEnabled) {
            devSettingsContainer.setVisibility(View.VISIBLE);
        } else {
            devSettingsContainer.setVisibility(View.GONE);
        }

        String refreshMode = prefs.getString("refresh_mode", "auto");
        if (refreshMode.equals("auto")) radioRefresh.check(R.id.radio_refresh_auto);
        else if (refreshMode.equals("manual")) radioRefresh.check(R.id.radio_refresh_manual);

        refreshButton.setAlpha(refreshMode.equals("manual") ? 1.0f : 0.4f);
        refreshButton.setEnabled(refreshMode.equals("manual"));
    }
}
