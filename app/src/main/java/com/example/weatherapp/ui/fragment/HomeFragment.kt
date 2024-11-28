package com.example.weatherapp.ui.fragment

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import org.json.JSONObject
import java.net.URL

/**
 * Fragment, který zobrazuje aktuální počasí z meteostanice na zahradě..
 */
class HomeFragmentDialog : DialogFragment() {

    private lateinit var temperatureTextView: TextView
    private lateinit var humidityTextView: TextView
    private lateinit var windSpeedTextView: TextView
    private lateinit var pressureTextView: TextView
    private lateinit var precipTextView: TextView
    private lateinit var obsTimeTextView: TextView
    private lateinit var cityNameTextView: TextView
    private lateinit var feelsLikeTextView : TextView

    var API = BuildConfig.HOME_API
    var WEATHER_STATION_ID = BuildConfig.STATION_ID
    private val apiUrl = "https://api.weather.com/v2/pws/observations/current?stationId=$WEATHER_STATION_ID&format=json&units=m&apiKey=$API"

    /**
     * Metoda, která vytvoří pohled fragmentu.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        updateTheme()
        return view
    }

    /**
     * Metoda, která se zavolá po vytvoření pohledu fragmentu.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTheme()

        temperatureTextView = view.findViewById(R.id.temperatureTextView)
        humidityTextView = view.findViewById(R.id.humidityTextView)
        windSpeedTextView = view.findViewById(R.id.windSpeedTextView)
        pressureTextView = view.findViewById(R.id.pressureTextView)
        precipTextView = view.findViewById(R.id.precipTextView)
        obsTimeTextView = view.findViewById(R.id.obsTimeTextView)
        cityNameTextView = view.findViewById(R.id.cityNameTextView)
        feelsLikeTextView = view.findViewById(R.id.feelsLikeTextView)

        fetchTemperatureData()
    }

    /**
     * Metoda, která zavolá asynchronní úlohu pro stažení dat o teplotě.
     */
    private fun fetchTemperatureData() {
        TemperatureTask().execute()
    }

    /**
     * Metoda, která aktualizuje téma fragmentu podle nastavení.
     */
    fun updateTheme() {
        val sharedPreferencesSettings = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferencesSettings.getString(SettingsFragment.PREF_THEME_KEY, SettingsFragment.THEME_PURPLE)
        val backgroundResource = if (currentTheme == SettingsFragment.THEME_PURPLE) R.drawable.gradient_purple_bg else R.drawable.gradient_green_bg

        // Najdeme ConstraintLayout v aktuálním pohledu a změníme jeho pozadí
        val constraintLayout = view?.findViewById<View>(R.id.mainContainer)
        constraintLayout?.setBackgroundResource(backgroundResource)
    }

    /**
     * Asynchronní vnitřní třída, která zpracovává stažení dat z api o počasí.
     */
    inner class TemperatureTask : AsyncTask<Void, Void, JSONObject?>() {
        override fun doInBackground(vararg params: Void?): JSONObject? {
            return try {
                val response = URL(apiUrl).readText(Charsets.UTF_8) // hodne ctu
                Log.d("HomeFragment", "API Response: $response")
                val jsonResponse = JSONObject(response)
                val observations = jsonResponse.optJSONArray("observations")
                if (observations == null || observations.length() == 0) {
                    return null
                }
                observations.getJSONObject(0) // prvni objekt je hned to, co mi plive json
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error parsing JSON", e)
                null
            }
        }

        /**
         * Aktualizuje UI po dokončení úlohy na pozadí.
         * @param result Získaná data ve formátu JSON.
         */
        override fun onPostExecute(result: JSONObject?) {
            updateTheme()
            super.onPostExecute(result)
            if (result != null) {
                try {
                    val metric = result.optJSONObject("metric")
                    if (metric != null) {
                        val cityName = result.optString("neighborhood", "Neznámé město")
                        val obsTime = result.optString("obsTimeLocal", "Neznámý čas")
                        val humidity = result.optInt("humidity", -1)
                        val temp = Math.round(metric.optDouble("temp", Double.NaN)).toString()
                        val feelsLike = metric.optDouble("windChill", Double.NaN)
                        val windSpeed = metric.optDouble("windSpeed", Double.NaN)
                        val pressure = metric.optDouble("pressure", Double.NaN)
                        val precip = metric.optDouble("precipTotal", Double.NaN)

                        // Překlad dle aktuálního jazyka
                        val language = SettingsFragment.selectedLanguage
                        if (language == "cs") {
                            feelsLikeTextView.text = "Pocitová teplota: $feelsLike°C"
                            obsTimeTextView.text = "Čas měření: $obsTime"
                            humidityTextView.text = "Vlhkost: $humidity%"
                            windSpeedTextView.text = "Rychlost větru: $windSpeed km/h"
                            pressureTextView.text = "Tlak vzduchu: $pressure hPa"
                            precipTextView.text = "Srážky: $precip mm"
                        } else {
                            feelsLikeTextView.text = "Feels like: $feelsLike°C"
                            obsTimeTextView.text = "Observation time: $obsTime"
                            humidityTextView.text = "Humidity: $humidity%"
                            windSpeedTextView.text = "Wind speed: $windSpeed km/h"
                            pressureTextView.text = "Pressure: $pressure hPa"
                            precipTextView.text = "Precipitation: $precip mm"
                        }
                        temperatureTextView.text = "$temp°C"
                        cityNameTextView.text = cityName
                    } else {
                        Toast.makeText(requireContext(), "Temperature data not available", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error processing data", e)
                    Toast.makeText(requireContext(), "Error processing data", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
