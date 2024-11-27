package com.example.weatherapp.ui.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherDatabase
import com.example.weatherapp.ui.adapters.Forecast
import com.example.weatherapp.ui.adapters.ForecastAdapter
import com.example.weatherapp.utils.WeatherDescriptionTranslator.Companion.translateDate
import com.example.weatherapp.utils.WeatherDescriptionTranslator.Companion.translateWeatherCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ForecastFragment : Fragment() {
    private lateinit var weatherDatabase: WeatherDatabase
    private lateinit var loader: ProgressBar
    private lateinit var mainContainer: RelativeLayout
    private lateinit var addressContainer: LinearLayout
    private lateinit var address: TextView
    private lateinit var errorText: TextView
    private lateinit var updatedAt: TextView
    private lateinit var forecastRecyclerView: RecyclerView

    private var weatherFragment = WeatherFragment()
    private var CITY = weatherFragment.CITY
    private var COUNTRY = weatherFragment.COUNTRY
    private val API = weatherFragment.API
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_weather_forecast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        CITY = sharedPreferences.getString("selected_city", "Prague") ?: "Prague"
        COUNTRY = sharedPreferences.getString("selected_country", "CZ") ?: "CZ"

        val sharedPreferencesSettings = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferencesSettings.getString(SettingsFragment.PREF_THEME_KEY, SettingsFragment.THEME_PURPLE)
        val backgroundResource = if (currentTheme == SettingsFragment.THEME_PURPLE) R.drawable.gradient_purple_bg else R.drawable.gradient_green_bg
        view.setBackgroundResource(backgroundResource)

        weatherDatabase = WeatherDatabase(requireContext())

        loader = view.findViewById(R.id.loader)
        mainContainer = view.findViewById(R.id.mainContainer)
        errorText = view.findViewById(R.id.errorText)
        addressContainer = view.findViewById(R.id.addressContainer)
        address = view.findViewById(R.id.address)
        updatedAt = view.findViewById(R.id.updated_at)

        forecastRecyclerView = view.findViewById(R.id.forecastRecyclerView)

        fetchData()
    }

    private fun fetchData() {
        val cachedData = weatherDatabase.getForecastWeather(CITY, COUNTRY)

        if (!isInternetAvailable()) {
            if (cachedData != null) {
                updateUIWithForecastData(cachedData)
                showToast(getString(R.string.no_connection))
            } else {
                // Žádná data nejsou dostupná
                showError(getString(R.string.no_connection))
            }
        } else {
            if (cachedData != null) {
                // Nejprve zobrazíme uložená data
                updateUIWithForecastData(cachedData)
            }
            // Následně načteme aktuální data
            coroutineScope.launch {
                fetchForecastData()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private suspend fun fetchForecastData() = withContext(Dispatchers.IO) {
        try {
            val response = URL("https://api.openweathermap.org/data/2.5/forecast?q=$CITY,$COUNTRY&units=metric&appid=$API").readText()
            weatherDatabase.insertOrUpdateForecastWeather(CITY, COUNTRY, response)
            withContext(Dispatchers.Main) {
                updateUIWithForecastData(response)
            }
        } catch (e: Exception) {
            Log.e("ForecastFragment", "Error fetching forecast data: ${e.message}")
            withContext(Dispatchers.Main) {
                val cachedData = weatherDatabase.getForecastWeather(CITY, COUNTRY)
                if (cachedData != null) {
                    updateUIWithForecastData(cachedData)
                    showToast(getString(R.string.no_connection))
                } else {
                    showError(getString(R.string.network_error))
                }
            }
        }
    }

    private fun updateUIWithForecastData(data: String) {
        try {
            val jsonObj = JSONObject(data)
            val list = jsonObj.getJSONArray("list")
            val forecastList = mutableListOf<Forecast>()
            val language = SettingsFragment.selectedLanguage

            for (i in 0 until list.length()) {
                val forecast = list.getJSONObject(i)
                val main = forecast.getJSONObject("main")
                val weather = forecast.getJSONArray("weather").getJSONObject(0)
                val dateTime = forecast.getLong("dt") * 1000
                val date = SimpleDateFormat("EEE, d MMM HH:mm", Locale.ENGLISH).format(Date(dateTime))
                val translatedDate = if (SettingsFragment.selectedLanguage == "cs") translateDate(date) else date

                // Přeložení popisu podmínek počasí
                val condition = if (language == "cs") {
                    translateWeatherCondition(weather.getString("description"))
                } else {
                    weather.getString("description").capitalize(Locale.getDefault())
                }

                val forecastItem = Forecast(
                    date = translatedDate,
                    temp = main.getString("temp"),
                    condition = condition,
                    iconRes = getWeatherIcon(weather.getString("icon"))
                )
                forecastList.add(forecastItem)
            }

            forecastRecyclerView.layoutManager = LinearLayoutManager(context)
            forecastRecyclerView.adapter = ForecastAdapter(forecastList)

            val lastUpdatedDate = SimpleDateFormat("EEE, d MMM HH:mm", Locale.ENGLISH).format(Date())
            address.text = "$CITY, $COUNTRY"
            updatedAt.text = if (language == "cs") "Aktualizováno: $lastUpdatedDate" else "Last Updated: $lastUpdatedDate"

            mainContainer.visibility = View.VISIBLE
            loader.visibility = View.GONE
        } catch (e: Exception) {
            showError("Nastala chyba při zpracování dat.")
        }
    }


    private fun getWeatherIcon(iconCode: String): Int {
        return when (iconCode) {
            "01d", "01n" -> R.drawable.clear_sky
            "02d", "02n" -> R.drawable.few_clouds
            "03d", "03n" -> R.drawable.scattered_clouds
            "04d", "04n" -> R.drawable.broken_clouds
            "09d", "09n" -> R.drawable.shower_rain
            "10d", "10n" -> R.drawable.rain
            "11d", "11n" -> R.drawable.thunderstorm
            "13d", "13n" -> R.drawable.snow
            "50d", "50n" -> R.drawable.mist
            else -> R.drawable.weather_icon
        }
    }


    private fun showError(message: String) {
        loader.visibility = View.GONE
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}
