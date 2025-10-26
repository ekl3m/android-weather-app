package com.example.weatherapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.R;
import com.example.weatherapp.models.HourlyForecastItem;

import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.HourlyViewHolder> {

    private List<HourlyForecastItem> forecastList;

    public HourlyForecastAdapter(List<HourlyForecastItem> forecastList) {
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public HourlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new HourlyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyViewHolder holder, int position) {
        HourlyForecastItem item = forecastList.get(position);
        holder.hourText.setText(item.getHour());
        holder.tempText.setText(item.getTemperature());
        holder.iconView.setImageResource(item.getIconResId());
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    public static class HourlyViewHolder extends RecyclerView.ViewHolder {
        TextView hourText, tempText;
        ImageView iconView;

        public HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            hourText = itemView.findViewById(R.id.text_hour);
            tempText = itemView.findViewById(R.id.text_temp);
            iconView = itemView.findViewById(R.id.image_icon);
        }
    }
}