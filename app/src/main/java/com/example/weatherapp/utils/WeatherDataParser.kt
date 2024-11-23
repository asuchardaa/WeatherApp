package com.example.weatherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.weatherapp.R
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeatherDataParser(private val data: String) {

    private val jsonObj: JSONObject = JSONObject(data)
    private val main = jsonObj.getJSONObject("main")
    private val sys = jsonObj.getJSONObject("sys")
    private val weather = jsonObj.getJSONArray("weather").getJSONObject(0)


    fun getUpdatedAtText(context: Context): String {
        val updatedAt: Long = jsonObj.getLong("dt")
        val updateAtString = context.getString(R.string.updated_at) + ": " +
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt * 1000))
        return updateAtString
    }


    fun getTemperature(): String {
        return "${Math.round(main.getDouble("temp"))}°C"
    }

    fun getMinTemperature(context: Context): String {
        val minTemp = main.getDouble("temp_min")
        val minTempString = context.getString(R.string.min_temp) + ": " + minTemp + "°C"
        return minTempString
    }

    fun getMaxTemperature(context: Context): String {
        val maxTemp = main.getDouble("temp_max")
        val maxTempString = context.getString(R.string.max_temp) + ": " + maxTemp + "°C"
        return maxTempString
    }

    fun getSunrise(): String {
        val sunrise: Long = sys.getLong("sunrise")
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sunrise * 1000))
    }

    fun getSunset(): String {
        val sunset: Long = sys.getLong("sunset")
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sunset * 1000))
    }

    fun getWind(): String {
        return "${jsonObj.getJSONObject("wind").getString("speed")} m/s"
    }

    fun getPressure(): String {
        return "${main.getString("pressure")} hPa"
    }

    fun getHumidity(): String {
        return "${main.getString("humidity")}%"
    }

    fun getClouds(): String {
        return "${jsonObj.getJSONObject("clouds").getString("all")}%"
    }

    fun getWeatherDescription(language: String): String {
        val description = weather.getString("description")
        val locale = Locale(language)
        return WeatherDescriptionTranslator.translate(description, locale)
    }




}
