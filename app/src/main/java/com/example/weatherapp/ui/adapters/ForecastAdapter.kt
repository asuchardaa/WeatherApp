package com.example.weatherapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R

class ForecastAdapter(private val forecastList: List<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecastList[position]
        holder.forecastDate.text = forecast.date
        holder.forecastTemp.text = "${forecast.temp}°C"
        holder.forecastCondition.text = forecast.condition
        // Set image based on weather condition (for example, set weather icon)
        holder.forecastIcon.setImageResource(forecast.iconRes)
    }

    override fun getItemCount(): Int = forecastList.size

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val forecastDate: TextView = view.findViewById(R.id.forecastDate)
        val forecastTemp: TextView = view.findViewById(R.id.forecastTemp)
        val forecastCondition: TextView = view.findViewById(R.id.forecastCondition)
        val forecastIcon: ImageView = view.findViewById(R.id.forecastIcon)
    }
}

data class Forecast(
    val date: String,
    val temp: String,
    val condition: String,
    val iconRes: Int
)
