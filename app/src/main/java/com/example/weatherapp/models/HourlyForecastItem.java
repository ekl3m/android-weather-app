package com.example.weatherapp.models;

public class HourlyForecastItem {
    private final String hour;
    private final String temperature;
    private final int iconResId;

    public HourlyForecastItem(String hour, String temperature, int iconResId) {
        this.hour = hour;
        this.temperature = temperature;
        this.iconResId = iconResId;
    }

    public String getHour() {
        return hour;
    }

    public String getTemperature() {
        return temperature;
    }

    public int getIconResId() {
        return iconResId;
    }
}