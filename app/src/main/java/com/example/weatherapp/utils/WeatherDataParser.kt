package com.example.weatherapp.utils

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeatherDataParser(private val data: String) {

    private val jsonObj: JSONObject = JSONObject(data)
    private val main = jsonObj.getJSONObject("main")
    private val sys = jsonObj.getJSONObject("sys")
    private val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

    fun getUpdatedAtText(): String {
        val updatedAt: Long = jsonObj.getLong("dt")
        return "Aktualizováno: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt * 1000))
    }

    fun getTemperature(): String {
        return "${Math.round(main.getDouble("temp"))}°C"
    }

    fun getMinTemperature(): String {
        return "Minimální teplota: ${Math.round(main.getDouble("temp_min"))}°C"
    }

    fun getMaxTemperature(): String {
        return "Maximální teplota: ${Math.round(main.getDouble("temp_max"))}°C"
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

    fun getWeatherDescription(): String {
        val description = weather.getString("description")
        return WeatherDescriptionTranslator.translate(description).capitalize(Locale.getDefault())
    }
}
