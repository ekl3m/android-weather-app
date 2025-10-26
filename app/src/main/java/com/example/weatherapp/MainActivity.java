package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.weatherapp.fragments.LocationsFragment;
import com.example.weatherapp.fragments.SettingsFragment;
import com.example.weatherapp.fragments.WeatherFragment;
import com.example.weatherapp.utils.WeatherApiClient;

public class MainActivity extends AppCompatActivity {

    private ImageView backgroundImageOld;
    private ImageView backgroundImageNew;
    private int lastWeatherBackgroundRes = R.drawable.bg_default2;
    private LinearLayout customBottomNav;
    private LinearLayout bannerConnection;
    private TextView bannerSubtitle;
    private final WeatherFragment weatherFragment = new WeatherFragment();
    private final android.os.Handler refreshHandler = new android.os.Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Edge-to-edge
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        backgroundImageOld = findViewById(R.id.background_image_old);
        backgroundImageNew = findViewById(R.id.background_image_new);
        customBottomNav = findViewById(R.id.custom_bottom_nav);
        bannerConnection = findViewById(R.id.banner_connection);
        bannerSubtitle = findViewById(R.id.banner_subtitle);

        ViewCompat.setOnApplyWindowInsetsListener(customBottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bannerConnection, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset + 16, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        setupNavigation();

        // Startowy fragment
        if (savedInstanceState == null) {
            loadFragment(weatherFragment);
            WeatherApiClient.fetchWeatherDataForSavedLocations(this);
            refreshHandler.postDelayed(refreshRunnable, 60_000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void setupNavigation() {
        ImageView home = findViewById(R.id.nav_home);
        ImageView loc = findViewById(R.id.nav_locations);
        ImageView settings = findViewById(R.id.nav_settings);

        View.OnClickListener listener = v -> {
            clearNavSelection();
            ((ImageView) v).setColorFilter(getColor(R.color.nav_active), android.graphics.PorterDuff.Mode.SRC_IN);

            Fragment selectedFragment;
            if (v.getId() == R.id.nav_home) {
                selectedFragment = weatherFragment;
            } else if (v.getId() == R.id.nav_locations) {
                selectedFragment = new LocationsFragment(); // Placeholder
            } else {
                selectedFragment = new SettingsFragment(); // Placeholder
            }

            loadFragment(selectedFragment);
        };

        home.setOnClickListener(listener);
        loc.setOnClickListener(listener);
        settings.setOnClickListener(listener);

        home.performClick();
    }

    private void clearNavSelection() {
        int def = getColor(R.color.nav_inactive);
        ((ImageView) findViewById(R.id.nav_home)).setColorFilter(def, android.graphics.PorterDuff.Mode.SRC_IN);
        ((ImageView) findViewById(R.id.nav_locations)).setColorFilter(def, android.graphics.PorterDuff.Mode.SRC_IN);
        ((ImageView) findViewById(R.id.nav_settings)).setColorFilter(def, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void loadFragment(Fragment fragment) {
        String tag = null;

        if (fragment instanceof WeatherFragment) {
            tag = "WEATHER_FRAGMENT";
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    public void crossfadeBackground(int newResId) {
        // Jeśli to nie tło z LocationsFragment (np. bg_black), to zapamiętaj
        if (newResId != R.drawable.bg_black) {
            lastWeatherBackgroundRes = newResId;
        }

        backgroundImageNew.setImageResource(newResId);
        backgroundImageNew.setAlpha(0f);
        backgroundImageNew.animate()
                .alpha(1f)
                .setDuration(1000)
                .withEndAction(() -> {
                    backgroundImageOld.setImageResource(newResId);
                    backgroundImageNew.setAlpha(0f);
                })
                .start();
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
            String mode = prefs.getString("refresh_mode", "auto");

            if (mode.equals("auto")) {
                WeatherApiClient.fetchWeatherDataForSavedLocations(MainActivity.this);
                refreshHandler.postDelayed(this, 60_000); // odświeżaj dalej tylko w trybie auto
            }
        }
    };

    public void showConnectionBanner(long lastSuccessfulUpdate, boolean lightMode, boolean noData) {
        runOnUiThread(() -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String time = sdf.format(new java.util.Date(lastSuccessfulUpdate));
            if (noData) {
                bannerSubtitle.setText("Brak danych pogodowych w pamięci urządzenia");
            } else {
                bannerSubtitle.setText("Ostatnia aktualizacja: " + time);
            }

            // Ustaw tło w zależności od trybu
            int backgroundRes = lightMode ? R.drawable.bg_blur_light : R.drawable.bg_blur_dark;
            bannerConnection.setBackgroundResource(backgroundRes);

            bannerConnection.setVisibility(View.VISIBLE);
        });
    }

    public void changeConnectionBannerBg(boolean lightMode) {
        runOnUiThread(() -> {
            int backgroundRes = lightMode ? R.drawable.bg_blur_light : R.drawable.bg_blur_dark;
            bannerConnection.setBackgroundResource(backgroundRes);
        });
    }

    public void hideConnectionBanner() {
        runOnUiThread(() -> bannerConnection.setVisibility(View.GONE));
    }

    public WeatherFragment getWeatherFragment() {
        return weatherFragment;
    }

    public int getLastWeatherBackgroundRes() {
        return lastWeatherBackgroundRes;
    }
}
