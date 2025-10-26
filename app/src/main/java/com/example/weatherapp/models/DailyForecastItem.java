package com.example.weatherapp.models;

public class DailyForecastItem {
    private final String day;
    private final String minTemp;
    private final String maxTemp;
    private final int iconResId;

    public DailyForecastItem(String day, String minTemp, String maxTemp, int iconResId) {
        this.day = day;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.iconResId = iconResId;
    }

    public String getDay() { return day; }
    public String getMinTemp() { return minTemp; }
    public String getMaxTemp() { return maxTemp; }
    public int getIconResId() { return iconResId; }
}