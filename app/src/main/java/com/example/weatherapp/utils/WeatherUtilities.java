package com.example.weatherapp.utils;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;

import com.example.weatherapp.MainActivity;
import com.example.weatherapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class WeatherUtilities {
    private WeatherUtilities() {}

    static public int parseTemp(String temp) {
        // Usuwa wszystko oprócz cyfr i minusów
        return Integer.parseInt(temp.replaceAll("[^\\d-]", ""));
    }

    static public double calculateDewPoint(double tempC, int humidityPercent) {
        double a = 17.27;
        double b = 237.7;
        double alpha = ((a * tempC) / (b + tempC)) + Math.log(humidityPercent / 100.0);
        return (b * alpha) / (a - alpha);
    }

    static public String getWindDirectionLabel(int deg) {
        String[] dirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        return dirs[(int) Math.round(((double) deg % 360) / 22.5) % 16];
    }

    static public boolean isNight(JSONObject response) {
        try {
            JSONObject sys = response.getJSONObject("sys");
            long sunrise = sys.getLong("sunrise");
            long sunset = sys.getLong("sunset");
            long currentTime = response.getLong("dt");

            return currentTime < sunrise || currentTime > sunset;
        } catch (JSONException e) {
            e.printStackTrace();
            return false; // domyślnie: dzień
        }
    }

    static public int getIconResId(int code, String iconCode) {
        boolean isNight = iconCode.endsWith("n");

        // Storm
        if (code >= 200 && code < 210) return R.drawable.cloud_bolt_rain_fill;
        if (code >= 210 && code <= 221) return R.drawable.cloud_bolt_fill;
        if (code > 221 && code < 300) return R.drawable.cloud_bolt_rain_fill;

        // Drizzle
        if (code >= 300 && code <= 500) return R.drawable.cloud_drizzle_fill;

        // Rain
        if (code >= 501 && code < 503) return R.drawable.cloud_rain_fill;
        if (code >= 503 && code < 511) return R.drawable.cloud_heavyrain_fill;
        if (code == 511) return R.drawable.snowflake;
        if (code > 511 && code <= 520) return R.drawable.cloud_rain_fill;
        if (code > 520 && code < 600) return R.drawable.cloud_heavyrain_fill;

        // Snow
        if (code == 600) return R.drawable.snowflake;
        if (code >= 601 && code < 611) return R.drawable.cloud_snow_fill;
        if (code >= 611 && code < 617) return R.drawable.cloud_sleet_fill;
        if (code >= 617 && code < 701) return R.drawable.cloud_snow_fill;

        // Atmosphere
        if (code >= 701 && code < 711) return R.drawable.cloud_fog_fill;
        if (code == 711) return R.drawable.smoke_fill;
        if (code > 711 && code <= 750) return R.drawable.cloud_fog_fill;
        if (code == 771) return R.drawable.wind;
        if (code == 781) return R.drawable.tornado;

        // Clear sky
        if (code == 800) return isNight ? R.drawable.moon_stars_fill : R.drawable.sun_max_fill;

        // Clouds
        if (code >= 801 && code < 803) return isNight ? R.drawable.cloud_moon_fill : R.drawable.cloud_sun_fill;
        if (code >= 803) return R.drawable.cloud_fill;

        return R.drawable.cloud_fill; // fallback
    }

    static public void setBackgroundForWeatherCode(Context context, int code, boolean isNight) {
        int bgRes;

        if (code >= 200 && code <= 232) {
            bgRes = isNight ? R.drawable.bg_storm_night : R.drawable.bg_storm;
        } else if (code >= 300 && code <= 321) {
            bgRes = isNight ? R.drawable.bg_rain_night : R.drawable.bg_rain;
        } else if (code >= 500 && code <= 504) {
            bgRes = isNight ? R.drawable.bg_rain_night : R.drawable.bg_rain;
        } else if (code == 511) {
            bgRes = isNight ? R.drawable.bg_snow_night : R.drawable.bg_snow;
        } else if (code >= 520 && code <= 531) {
            bgRes = isNight ? R.drawable.bg_rain_night : R.drawable.bg_rain;
        } else if (code >= 600 && code <= 622) {
            bgRes = isNight ? R.drawable.bg_snow_night : R.drawable.bg_snow;
        } else if (code >= 701 && code <= 762) {
            bgRes = isNight ? R.drawable.bg_fog_night : R.drawable.bg_fog;
        } else if (code == 771 || code == 781) {
            bgRes = isNight ? R.drawable.bg_wind_night : R.drawable.bg_wind;
        } else if (code == 800) {
            bgRes = isNight ? R.drawable.bg_cloudless_sky_night : R.drawable.bg_sun;
        } else if (code >= 801 && code <= 802) {
            bgRes = isNight ? R.drawable.bg_stars_clouds : R.drawable.bg_sun_clouds;
        } else if (code >= 803 && code <= 804) {
            bgRes = isNight ? R.drawable.bg_clouds_night : R.drawable.bg_clouds;
        } else {
            bgRes = isNight ? R.drawable.bg_clouds_night : R.drawable.bg_clouds; // fallback
        }

        if (context instanceof MainActivity) {
            ((MainActivity) context).crossfadeBackground(bgRes);
        }
    }

    static public int getBackgroundResId(int code, boolean isNight) {
        if (code >= 200 && code <= 232)
            return isNight ? R.drawable.bg_storm_night : R.drawable.bg_storm;
        if (code >= 300 && code <= 321)
            return isNight ? R.drawable.bg_rain_night : R.drawable.bg_rain;
        if (code >= 500 && code <= 504)
            return isNight ? R.drawable.bg_rain_night : R.drawable.bg_rain;
        if (code == 511)
            return isNight ? R.drawable.bg_snow_night : R.drawable.bg_snow;
        if (code >= 520 && code <= 531)
            return isNight ? R.drawable.bg_rain_night : R.drawable.bg_rain;
        if (code >= 600 && code <= 622)
            return isNight ? R.drawable.bg_snow_night : R.drawable.bg_snow;
        if (code >= 701 && code <= 762)
            return isNight ? R.drawable.bg_fog_night : R.drawable.bg_fog;
        if (code == 771 || code == 781)
            return isNight ? R.drawable.bg_wind_night : R.drawable.bg_wind;
        if (code == 800)
            return isNight ? R.drawable.bg_cloudless_sky_night : R.drawable.bg_sun;
        if (code >= 801 && code <= 802)
            return isNight ? R.drawable.bg_stars_clouds : R.drawable.bg_sun_clouds;
        if (code >= 803 && code <= 804)
            return isNight ? R.drawable.bg_clouds_night : R.drawable.bg_clouds;

        return R.drawable.bg_default2; // fallback
    }

    static public String getNiceDescriptionFromCode(int code, boolean isNight) {
        if (code >= 200 && code <= 232) return "Burza";
        if (code >= 300 && code <= 321) return "Mżawka";
        if (code >= 500 && code <= 504) return "Deszcz";
        if (code == 511) return "Marznący deszcz";
        if (code >= 520 && code <= 531) return "Przelotny deszcz";

        if (code >= 600 && code <= 602) return "Śnieg";
        if (code >= 611 && code <= 616) return "Deszcz ze śniegiem";
        if (code >= 620 && code <= 622) return "Przelotne opady śniegu";

        if (code >= 701 && code <= 741) return "Mgła";

        if (code == 771 || code == 781) return "Wietrznie";

        if (code == 800) {
            return isNight ? "Bezchmurne niebo" : "Słonecznie";
        }
        if (code == 801) return "Lekkie chmury";
        if (code == 802) return "Pojedyncze chmury";
        if (code == 803) return "Umiarkowane chmury";
        if (code == 804) return "Głównie chmury";

        return "Nieznana pogoda";
    }

    static public String getVisibilityLabel(int km) {
        if (km >= 10) return "Idealna\nwidoczność.";
        if (km >= 6) return "Bardzo dobra\nwidoczność.";
        if (km >= 3) return "Dobra\nwidoczność.";
        if (km >= 1) return "Ograniczona\nwidoczność.";
        return "Bardzo słaba\nwidoczność.";
    }

    static public String getAQIDescription(int aqi) {
        switch (aqi) {
            case 1: return "Bardzo dobra";
            case 2: return "Dobra";
            case 3: return "Umiarkowana";
            case 4: return "Zła";
            case 5: return "Bardzo zła";
            default: return "Brak danych";
        }
    }

    static public void testInternetWithWeatherCall(Context context, Runnable onFailure, Runnable onSuccess) {
        WeatherApiClient.getWeather(context, "Warsaw", "metric", new WeatherApiClient.WeatherCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(String error) {
                if (onFailure != null) onFailure.run();
            }
        });
    }
}
