package com.example.weatherapp.ui.fragment

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun fetchTemperatureData() {
        TemperatureTask().execute()
    }

    inner class TemperatureTask : AsyncTask<Void, Void, JSONObject?>() {
        override fun doInBackground(vararg params: Void?): JSONObject? {
            return try {
                val response = URL(apiUrl).readText(Charsets.UTF_8)
                Log.d("HomeFragment", "API Response: $response")
                val jsonResponse = JSONObject(response)
                val observations = jsonResponse.optJSONArray("observations")
                if (observations == null || observations.length() == 0) {
                    return null
                }
                observations.getJSONObject(0)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error parsing JSON", e)
                null
            }
        }

        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)
            if (result != null) {
                try {
                    // tady bacha na api response, metric x imperial (lisi se dle jednotek, ale me imo by mohlo stacit metric a zbytek si dobrat sam...)
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

                        temperatureTextView.text = "$temp°C"
                        feelsLikeTextView.text = "Pocitová teplota: $feelsLike°C"
                        obsTimeTextView.text = "Čas měření: $obsTime"
                        humidityTextView.text = "Vlhkost: $humidity%"
                        windSpeedTextView.text = "Rychlost větru: $windSpeed km/h"
                        pressureTextView.text = "Tlak vzduchu: $pressure hPa"
                        precipTextView.text = "Srážky: $precip mm"
                        cityNameTextView.text = cityName
                    } else {
                        Toast.makeText(requireContext(), "Data pro teplotu nejsou dostupná", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error processing data", e)
                    Toast.makeText(requireContext(), "Chyba při zpracování dat", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Chyba při načítání dat", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
