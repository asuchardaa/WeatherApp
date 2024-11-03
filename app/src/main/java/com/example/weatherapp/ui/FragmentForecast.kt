package com.example.weatherapp.ui

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class FragmentForecast : Fragment() {
    private lateinit var weatherDatabase: WeatherDatabase
    private lateinit var loader: ProgressBar
    private lateinit var mainContainer: RelativeLayout
    private lateinit var errorText: TextView
    private var fragmentWeather = FragmentWeather()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_weather_forecast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Načtení argumentů
        arguments?.let {
            fragmentWeather.CITY = it.getString("CITY") ?: "Prague" // Zadejte výchozí město
            fragmentWeather.COUNTRY = it.getString("COUNTRY") ?: "CZ" // Zadejte výchozí zemi
        }

        weatherDatabase = WeatherDatabase(requireContext())
        loader = view.findViewById(R.id.loader)
        mainContainer = view.findViewById(R.id.mainContainer)
        errorText = view.findViewById(R.id.errorText)

        fetchData()
    }

    private fun fetchData() {
        val cachedData = weatherDatabase.getWeatherData(fragmentWeather.CITY, fragmentWeather.COUNTRY)
        if (cachedData != null) {
            updateUIWithForecastData(cachedData)
        } else {
            ForecastTask().execute()
        }
    }

    private fun updateUIWithForecastData(data: String) {
        if (data != null) {
            try {
                val jsonObj = JSONObject(data)
                val list = jsonObj.getJSONArray("list")
                val forecasts = StringBuilder()

                for (i in 0 until list.length()) {
                    val forecast = list.getJSONObject(i)
                    val main = forecast.getJSONObject("main")
                    val weather = forecast.getJSONArray("weather").getJSONObject(0)
                    val dateTime = forecast.getLong("dt") * 1000
                    val date = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(Date(dateTime))

                    forecasts.append("$date: ${main.getString("temp")}°C - ${weather.getString("description").capitalize(Locale.getDefault())}\n")
                }

                view?.findViewById<TextView>(R.id.forecast)?.text = forecasts.toString()
                view?.findViewById<TextView>(R.id.address)?.text = "$fragmentWeather.CITY,$fragmentWeather.COUNTRY"
                mainContainer.visibility = View.VISIBLE
                loader.visibility = View.GONE
            } catch (e: Exception) {
                showError("Nastala chyba při zpracování dat.")
            }
        }
    }

    private fun showError(message: String) {
        loader.visibility = View.GONE
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private inner class ForecastTask : AsyncTask<Void, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
            mainContainer.visibility = View.GONE
            errorText.visibility = View.GONE
        }

        override fun doInBackground(vararg params: Void?): String? {
            return try {
                val response = URL("https://api.openweathermap.org/data/2.5/forecast?q=$fragmentWeather.CITY,$fragmentWeather.COUNTRY&appid=${BuildConfig.API_KEY}&units=metric").readText()
                weatherDatabase.insertOrUpdateWeatherData(fragmentWeather.CITY, fragmentWeather.COUNTRY, response)
                response
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                updateUIWithForecastData(result)
            } else {
                showError("Nastala chyba při načítání počasí. Zkontrolujte připojení k internetu.")
            }
        }
    }
}
