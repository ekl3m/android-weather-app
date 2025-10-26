package com.example.weatherapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.weatherapp.MainActivity;
import com.example.weatherapp.R;
import com.example.weatherapp.utils.WeatherApiClient;
import com.example.weatherapp.utils.WeatherCacheManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.widget.Toast;

import com.example.weatherapp.utils.WeatherUtilities;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public class LocationsFragment extends Fragment {

    private EditText searchField;
    private LinearLayout suggestionsContainer;
    private LinearLayout savedLocationsContainer;
    private View savedLocationsBlock;
    private TextView cancelButton;
    private View rootView;
    private static final String PREFS_NAME = "locations_prefs";
    private static final String KEY_SAVED = "saved_locations";
    private final android.os.Handler handler = new android.os.Handler();
    private final Map<String, View> loadedCardsMap = new HashMap<>();
    private final Map<String, View> displayedCardsMap = new HashMap<>();
    private final List<String> expectedCities = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_locations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchField = view.findViewById(R.id.input_search_city);
        suggestionsContainer = view.findViewById(R.id.suggestions_container);
        savedLocationsContainer = view.findViewById(R.id.saved_locations_container);
        savedLocationsBlock = view.findViewById(R.id.saved_locations_block);
        savedLocationsBlock.setVisibility(View.VISIBLE);
        cancelButton = view.findViewById(R.id.button_cancel);
        rootView = view;
        View clearButton = view.findViewById(R.id.icon_clear); // ImageView

        loadSavedLocations();

        ((MainActivity) requireActivity()).crossfadeBackground(R.drawable.bg_black);

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.scroll_view_locations), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset + 80, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Obsługa X (czyści)
        clearButton.setOnClickListener(v -> searchField.setText(""));

        // Obsługa Anuluj
        cancelButton.setOnClickListener(v -> {
            searchField.setText("");
            searchField.clearFocus();
            cancelButton.setVisibility(View.GONE);
            clearButton.setVisibility(View.GONE);
            suggestionsContainer.removeAllViews();
            savedLocationsBlock.setAlpha(0f);
            savedLocationsBlock.setVisibility(View.VISIBLE);
            savedLocationsBlock.animate().alpha(1f).setDuration(200).start();

            // Schowaj klawiaturę
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        });

        searchField.setOnFocusChangeListener((v, hasFocus) -> {
            cancelButton.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            if (hasFocus) {
                savedLocationsBlock.animate().alpha(0f).setDuration(200).withEndAction(() -> savedLocationsBlock.setVisibility(View.GONE));
            } else if (searchField.getText().toString().length() < 3) {
                savedLocationsBlock.setAlpha(0f);
                savedLocationsBlock.setVisibility(View.VISIBLE);
                savedLocationsBlock.animate().alpha(1f).setDuration(200).start();
            }
        });

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (s.length() > 2) {
                    fetchSuggestions(s.toString());
                } else {
                    suggestionsContainer.removeAllViews();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAutoRefreshOnInDevSettings()) {
            handler.post(refreshRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;

            SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
            String refreshMode = prefs.getString("refresh_mode", "auto");
            if (refreshMode.equals("auto")) {
                loadSavedLocations();
                handler.postDelayed(this, 20_000);
            }
        }
    };

    private void fetchSuggestions(String query) {
        WeatherApiClient.getCitySuggestions(requireContext(), query, new WeatherApiClient.CoordinatesListCallback() {
            @Override
            public void onSuccess(JSONArray cities) {
                suggestionsContainer.removeAllViews();
                Set<String> uniqueLocations = new HashSet<>();
                for (int i = 0; i < cities.length(); i++) {
                    JSONObject city = cities.optJSONObject(i);
                    if (city == null) continue;

                    JSONObject localNames = city.optJSONObject("local_names");
                    String name = city.optString("name", "");
                    if (localNames != null && localNames.has("pl")) {
                        name = localNames.optString("pl");
                    }
                    String country = city.optString("country", "");
                    String state = city.optString("state", "");

                    String key = name + "|" + state + "|" + country;
                    if (uniqueLocations.contains(key)) continue;
                    uniqueLocations.add(key);

                    TextView suggestion = new TextView(requireContext());
                    suggestion.setText("" + name + (state.isEmpty() ? "" : ", " + state) + ", " + country);
                    suggestion.setPadding(24, 16, 24, 16);
                    suggestion.setTextColor(getResources().getColor(R.color.white));

                    String originalName = city.optString("name", ""); // oryginalna nazwa do zapisu
                    String finalName = name;
                    suggestion.setOnClickListener(v -> {
                        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        String savedList = prefs.getString(KEY_SAVED, "[]");

                        try {
                            JSONArray array = new JSONArray(savedList);
                            // Nowy zapis jako obiekt { "query": ..., "name": ... }
                            boolean alreadySaved = false;
                            for (int j = 0; j < array.length(); j++) {
                                JSONObject entry = array.optJSONObject(j);
                                if (entry != null && entry.has("query")) {
                                    if (entry.getString("query").equalsIgnoreCase(originalName)) {
                                        alreadySaved = true;
                                        break;
                                    }
                                }
                            }

                            if (!alreadySaved) {
                                JSONObject entry = new JSONObject();
                                entry.put("query", originalName);
                                entry.put("name", finalName);
                                array.put(entry);

                                if (!expectedCities.contains(originalName)) {
                                    expectedCities.add(originalName);
                                }
                                fetchCardForCity(entry.toString(), originalName);
                            }

                            prefs.edit()
                                    .putString(KEY_SAVED, array.toString())
                                    .apply();

                            searchField.setText("");
                            suggestionsContainer.removeAllViews();

                            cancelButton.performClick();

                            rootView.findViewById(R.id.scroll_view_locations).post(() ->
                                    rootView.findViewById(R.id.scroll_view_locations).scrollTo(0, rootView.findViewById(R.id.scroll_view_locations).getBottom())
                            );

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });

                    suggestionsContainer.addView(suggestion);
                }
            }

            @Override
            public void onError(String error) {
                suggestionsContainer.removeAllViews();
                TextView errorText = new TextView(requireContext());
                errorText.setText(error);
                errorText.setPadding(24, 16, 24, 16);
                errorText.setTextColor(getResources().getColor(R.color.gray_with_alpha));
                suggestionsContainer.addView(errorText);
            }
        });
    }

    private void loadSavedLocations() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedList = prefs.getString(KEY_SAVED, "[]");
        savedLocationsContainer.removeAllViews();
        expectedCities.clear();
        loadedCardsMap.clear();

        try {
            JSONArray array = new JSONArray(savedList);

            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) continue;
                String cityQuery = entry.optString("query", "");
                if (cityQuery.isEmpty()) continue;

                expectedCities.add(cityQuery);
                fetchCardForCity(entry.toString(), cityQuery);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fetchCardForCity(String entryJson, String cityQuery) {
        WeatherUtilities.testInternetWithWeatherCall(requireContext(),
                () -> {
                    JSONObject cachedData = WeatherCacheManager.loadCityWeatherCardCache(requireContext(), cityQuery);
                    long ts = WeatherCacheManager.getCityWeatherCardTimestamp(requireContext(), cityQuery);
                    if (cachedData != null) {
                        View card = createCityCardFromCache(entryJson, cachedData);
                        loadedCardsMap.put(cityQuery, card);
                        tryDisplayAllCards();

                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showConnectionBanner(ts, true, false);
                        }
                    } else {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showConnectionBanner(ts, true, true);
                        }
                    }
                },
                () -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideConnectionBanner();
                    }
                    createCityCardWithApiData(entryJson, cityQuery, card -> {
                        loadedCardsMap.put(cityQuery, card);
                        tryDisplayAllCards();
                    });
                }
        );
    }

    private void tryDisplayAllCards() {
        if (loadedCardsMap.size() < expectedCities.size()) return;

        for (String city : expectedCities) {
            View newCard = loadedCardsMap.get(city);
            if (newCard != null) {
                crossfadeCard(city, newCard);
            }
        }
    }

    // Tworzy kartę lokalizacji na podstawie danych z cache
    private View createCityCardFromCache(String entryJson, JSONObject cachedData) {
        try {
            JSONObject entry = new JSONObject(entryJson);
            String cityQuery = entry.getString("query");
            String displayName = entry.optString("name", cityQuery);

            JSONObject weather = cachedData.optJSONObject("weather");
            JSONObject daily = cachedData.optJSONObject("daily");

            if (weather == null || daily == null) {
                Log.e("LOCATIONS_FRAG", "Niepoprawny cache: brak weather lub daily");
                return null;
            }

            SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            String tempUnit = prefs.getString("unit_temp", "celsius");

            JSONObject main = weather.getJSONObject("main");
            double rawTemp = main.getDouble("temp");
            double temperature = tempUnit.equals("fahrenheit") ? (rawTemp * 9 / 5) + 32 : rawTemp;
            String formattedTemp = String.format(Locale.getDefault(), "%.0f°", temperature);

            JSONArray weatherArray = weather.getJSONArray("weather");
            JSONObject weatherItem = weatherArray.getJSONObject(0);
            int weatherCode = weatherItem.getInt("id");
            String description = WeatherUtilities.getNiceDescriptionFromCode(weatherCode, WeatherUtilities.isNight(weather));
            int backgroundResId = WeatherUtilities.getBackgroundResId(weatherCode, WeatherUtilities.isNight(weather));

            long utcTimestamp = weather.getLong("dt");
            int timezoneOffset = weather.getInt("timezone");
            long localTimeMillis = (utcTimestamp + timezoneOffset) * 1000L;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String hour = sdf.format(new Date(localTimeMillis));

            JSONObject today = daily.getJSONArray("list").getJSONObject(0);
            JSONObject temp = today.getJSONObject("temp");
            double min = tempUnit.equals("fahrenheit") ? (temp.getDouble("min") * 9 / 5) + 32 : temp.getDouble("min");
            double max = tempUnit.equals("fahrenheit") ? (temp.getDouble("max") * 9 / 5) + 32 : temp.getDouble("max");
            String tempRange = "Od " + String.format(Locale.getDefault(), "%.0f°", min) + " do " + String.format(Locale.getDefault(), "%.0f°", max);

            View card = createCityCard(
                    displayName,
                    hour,
                    description,
                    formattedTemp,
                    tempRange,
                    backgroundResId,
                    cityQuery
            );

            // Fade-in animacja
            card.setAlpha(0f);
            card.animate().alpha(1f).setDuration(400).start();

            return card;

        } catch (Exception e) {
            Log.e("LOCATIONS_FRAG", "Błąd tworzenia karty z cache (fade): " + e.getMessage());
            return null;
        }
    }

    // Tworzy kartę lokalizacji na podstawie danych z API
    private void createCityCardWithApiData(String entryJson, String cityQuery, Consumer<View> callback) {
        try {
            JSONObject entry = new JSONObject(entryJson);
            String queryNoWhitespace = cityQuery.trim();
            String name = entry.optString("name", cityQuery);

            SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            String tempUnit = prefs.getString("unit_temp", "celsius");

            WeatherApiClient.getWeather(requireContext(), queryNoWhitespace, "metric", new WeatherApiClient.WeatherCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (!isAdded() || getActivity() == null) return;

                    try {
                        JSONObject main = response.getJSONObject("main");
                        double rawTemp = main.getDouble("temp");
                        double temperature = tempUnit.equals("fahrenheit") ? (rawTemp * 9 / 5) + 32 : rawTemp;
                        String formattedTemp = String.format(Locale.getDefault(), "%.0f°", temperature);

                        JSONArray weatherArray = response.getJSONArray("weather");
                        JSONObject weather = weatherArray.getJSONObject(0);
                        int weatherCode = weather.getInt("id");

                        String description = WeatherUtilities.getNiceDescriptionFromCode(weatherCode, WeatherUtilities.isNight(response));

                        long now = System.currentTimeMillis();
                        int timezoneOffset = response.getInt("timezone");
                        long targetTimeMillis = now + (timezoneOffset * 1000L);

                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String hour = sdf.format(new Date(targetTimeMillis));

                        int backgroundResId = WeatherUtilities.getBackgroundResId(weatherCode, WeatherUtilities.isNight(response));

                        // Forecast (min-max)
                        WeatherApiClient.getDailyForecast(requireContext(), queryNoWhitespace, "metric", new WeatherApiClient.WeatherCallback() {
                            @Override
                            public void onSuccess(JSONObject dailyResponse) {
                                try {
                                    JSONObject today = dailyResponse.getJSONArray("list").getJSONObject(0);
                                    JSONObject temp = today.getJSONObject("temp");

                                    double min = tempUnit.equals("fahrenheit")
                                            ? (temp.getDouble("min") * 9 / 5) + 32
                                            : temp.getDouble("min");
                                    double max = tempUnit.equals("fahrenheit")
                                            ? (temp.getDouble("max") * 9 / 5) + 32
                                            : temp.getDouble("max");

                                    String tempRange = "Od " +
                                            String.format(Locale.getDefault(), "%.0f°", min) +
                                            " do " +
                                            String.format(Locale.getDefault(), "%.0f°", max);

                                    requireActivity().runOnUiThread(() -> {
                                        View card = createCityCard(
                                                name,
                                                hour,
                                                description,
                                                formattedTemp,
                                                tempRange,
                                                backgroundResId,
                                                cityQuery
                                        );

                                        // Fade-in animacja
                                        card.setAlpha(0f);
                                        card.animate().alpha(1f).setDuration(400).start();

                                        callback.accept(card); // << przekazanie dalej
                                    });

                                } catch (JSONException e) {
                                    Log.e("LOCATIONS_FRAG", "Błąd parsowania daily: " + e.getMessage());
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("LOCATIONS_FRAG", "Błąd daily forecast: " + error);
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Nie udało się pobrać prognozy.", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                        });

                    } catch (JSONException e) {
                        Log.e("LOCATIONS_FRAG", "Błąd parsowania response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("LOCATIONS_FRAG", "Błąd danych pogodowych: " + error);
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Nie udało się pobrać danych.", Toast.LENGTH_SHORT).show();
                        // ewentualnie obsłuż usuwanie z listy jak wcześniej
                    });
                }
            });

        } catch (JSONException e) {
            Log.e("LOCATIONS_FRAG", "Błąd JSON w createCityCardWithApiData: " + e.getMessage());
        }
    }


    // cityName = display name, originalName = query
    private View createCityCard(String cityName, String hour, String description, String temp, String tempRange, int backgroundResId, String originalName) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View cardView = inflater.inflate(R.layout.card_location, savedLocationsContainer, false);

        // Ustawienia tła pogodowego
        ImageView background = cardView.findViewById(R.id.image_background);
        background.setImageResource(backgroundResId);

        // Ustawienia tekstów
        ((TextView) cardView.findViewById(R.id.text_city_name)).setText(cityName);
        ((TextView) cardView.findViewById(R.id.text_city_hour)).setText(hour);
        ((TextView) cardView.findViewById(R.id.text_city_description)).setText(description);
        ((TextView) cardView.findViewById(R.id.text_city_temp)).setText(temp);
        ((TextView) cardView.findViewById(R.id.text_city_range)).setText(tempRange);

        // Domyślne tło
        cardView.setBackgroundResource(R.drawable.card_normal_border);

        // Obsługa kliknięcia (ustawienie jako aktualnie wybranego miasta)
        cardView.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("active_city", originalName)
                    .apply();

            // Ustaw targetCity we WeatherFragment
            ((MainActivity) requireActivity()).getWeatherFragment().setTargetCity(cityName);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new WeatherFragment())
                    .commit();

            // Podświetl ikonę Home, odznacz Locations
            ((ImageView) requireActivity().findViewById(R.id.nav_home))
                    .setColorFilter(requireContext().getColor(R.color.nav_active), android.graphics.PorterDuff.Mode.SRC_IN);
            ((ImageView) requireActivity().findViewById(R.id.nav_locations))
                    .setColorFilter(requireContext().getColor(R.color.nav_inactive), android.graphics.PorterDuff.Mode.SRC_IN);
        });

        cardView.setOnLongClickListener(v2 -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedList = prefs.getString(KEY_SAVED, "[]");
            try {
                JSONArray array = new JSONArray(savedList);
                if (array.length() <= 1) {
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Nie można usunąć")
                            .setMessage("Musisz mieć co najmniej jedno zapisane miasto.")
                            .setPositiveButton("OK", null)
                            .show();
                    return true;
                }

                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Usuń lokalizację")
                        .setMessage("Czy na pewno chcesz usunąć \"" + cityName + "\" z zapisanych?")
                        .setPositiveButton("Usuń", (dialog, which) -> {
                            JSONArray updatedArray = new JSONArray();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject item = array.optJSONObject(i);
                                if (item != null && item.has("query")) {
                                    try {
                                        if (!item.getString("query").equalsIgnoreCase(originalName)) {
                                            updatedArray.put(item);
                                        }
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                            prefs.edit()
                                    .putString(KEY_SAVED, updatedArray.toString())
                                    .apply();

                            expectedCities.remove(originalName);
                            loadedCardsMap.remove(originalName);
                            displayedCardsMap.remove(originalName);

                            loadSavedLocations();
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        });

        return cardView;
    }

    private void crossfadeCard(String city, View newCard) {
        View oldCard = displayedCardsMap.get(city);

        if (oldCard != null) {
            // Fade out starej
            oldCard.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        int index = savedLocationsContainer.indexOfChild(oldCard);
                        savedLocationsContainer.removeView(oldCard);

                        // Fade in nowej
                        newCard.setAlpha(0f);
                        savedLocationsContainer.addView(newCard, index);
                        newCard.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .start();

                        displayedCardsMap.put(city, newCard);
                    })
                    .start();
        } else {
            // Nie było starej – po prostu dodaj z fade in
            newCard.setAlpha(0f);
            savedLocationsContainer.addView(newCard);
            newCard.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start();

            displayedCardsMap.put(city, newCard);
        }
    }

    private boolean isAutoRefreshOnInDevSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String refreshMode = prefs.getString("refresh_mode", "auto");
        return refreshMode.equals("auto");
    }
}
