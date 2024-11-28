package com.example.weatherapp.utils

import android.content.Context
import com.example.weatherapp.R
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Třída `WeatherDataParser` poskytuje metody pro parsování dat o počasí ve formátu JSON.
 *
 * @property data JSON data získaná z API.
 */
class WeatherDataParser(private val data: String) {

    private val jsonObj: JSONObject = JSONObject(data)
    private val main = jsonObj.getJSONObject("main")
    private val sys = jsonObj.getJSONObject("sys")
    private val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

    /**
     * Vrací text s informací o poslední aktualizaci počasí.
     *
     * @param context Kontext pro získání lokalizovaného textu.
     * @return Text s datem a časem poslední aktualizace.
     */
    fun getUpdatedAtText(context: Context): String {
        val updatedAt: Long = jsonObj.getLong("dt")
        val updateAtString = context.getString(R.string.updated_at) + ": " +
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt * 1000))
        return updateAtString
    }

    /**
     * Vrací aktuální teplotu ve formátu "X°C".
     */
    fun getTemperature(): String {
        return "${Math.round(main.getDouble("temp"))}°C"
    }

    /**
     * Vrací minimální teplotu ve formátu "Minimální teplota: X°C".
     *
     * @param context Kontext pro získání lokalizovaného textu.
     */
    fun getMinTemperature(context: Context): String {
        val minTemp = main.getDouble("temp_min")
        val minTempString = context.getString(R.string.min_temp) + ": " + minTemp + "°C"
        return minTempString
    }

    /**
     * Vrací maximální teplotu ve formátu "Maximální teplota: X°C".
     *
     * @param context Kontext pro získání lokalizovaného textu.
     */
    fun getMaxTemperature(context: Context): String {
        val maxTemp = main.getDouble("temp_max")
        val maxTempString = context.getString(R.string.max_temp) + ": " + maxTemp + "°C"
        return maxTempString
    }

    /**
     * Vrací čas východu slunce ve formátu "HH:mm".
     */
    fun getSunrise(): String {
        val sunrise: Long = sys.getLong("sunrise")
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sunrise * 1000))
    }

    /**
     * Vrací čas západu slunce ve formátu "HH:mm".
     */
    fun getSunset(): String {
        val sunset: Long = sys.getLong("sunset")
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sunset * 1000))
    }

    /**
     * Vrací rychlost větru ve formátu "X m/s".
     */
    fun getWind(): String {
        return "${jsonObj.getJSONObject("wind").getString("speed")} m/s"
    }

    /**
     * Vrací tlak vzduchu ve formátu "X hPa".
     */
    fun getPressure(): String {
        return "${main.getString("pressure")} hPa"
    }

    /**
     * Vrací vlhkost ve formátu "X%".
     */
    fun getHumidity(): String {
        return "${main.getString("humidity")}%"
    }

    /**
     * Vrací oblačnost ve formátu "X%".
     */
    fun getClouds(): String {
        return "${jsonObj.getJSONObject("clouds").getString("all")}%"
    }

    /**
     * Vrací popis počasí v závislosti na zvoleném jazyce.
     *
     * @param language Jazyk, ve kterém má být popis počasí vrácen.
     */
    fun getWeatherDescription(language: String): String {
        val description = weather.getString("description")
        val locale = Locale(language)
        return WeatherDescriptionTranslator.translate(description, locale)
    }
}
