package com.example.weatherapp.utils;

public class UnitConverter {

    public static String convertTemperature(double tempCelsius, String targetUnit) {
        if (targetUnit.equals("fahrenheit")) {
            return String.format("%.0f°", (tempCelsius * 9 / 5) + 32);
        }
        return String.format("%.0f°", tempCelsius); // domyślnie °C
    }

    public static String convertWind(double windMps, String targetUnit) {
        switch (targetUnit) {
            case "kph":
                return String.format("%.1f km/h", windMps * 3.6);
            case "mph":
                return String.format("%.1f mph", windMps * 2.237);
            default:
                return String.format("%.1f m/s", windMps);
        }
    }

    public static String convertPressure(int pressureHpa, String targetUnit) {
        if ("mmHg".equals(targetUnit)) {
            return String.format("%.0f", pressureHpa * 0.75006);
        }
        return String.valueOf(pressureHpa);
    }

    public static String getPressureUnitLabel(String targetUnit) {
        return "mmHg".equals(targetUnit) ? "mmHg" : "hPa";
    }

    public static String convertVisibility(int visibilityMeters, String targetUnit) {
        double visibilityKm = visibilityMeters / 1000.0;
        if (targetUnit.equals("miles")) {
            return String.format("%.0f mil", visibilityKm * 0.621371);
        }
        return String.format("%.0f km", visibilityKm);
    }
}