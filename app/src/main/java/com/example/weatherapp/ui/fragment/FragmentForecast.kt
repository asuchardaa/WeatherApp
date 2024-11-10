package com.example.weatherapp.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherDatabase
import com.example.weatherapp.ui.adapter.Forecast
import com.example.weatherapp.ui.adapter.ForecastAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class FragmentForecast : Fragment() {
    private lateinit var weatherDatabase: WeatherDatabase
    private lateinit var loader: ProgressBar
    private lateinit var mainContainer: RelativeLayout
    private lateinit var addressContainer: LinearLayout
    private lateinit var address: TextView
    private lateinit var errorText: TextView
    private lateinit var updatedAt: TextView
    private lateinit var forecastRecyclerView: RecyclerView

    private var fragmentWeather = FragmentWeather()
    private var CITY = fragmentWeather.CITY
    private var COUNTRY = fragmentWeather.COUNTRY
    private val API = fragmentWeather.API
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_weather_forecast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        CITY = sharedPreferences.getString("selected_city", "Prague") ?: "Prague"
        COUNTRY = fragmentWeather.cityCountryMap[CITY] ?: "CZ"

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
        if (cachedData != null) {
            updateUIWithForecastData(cachedData)
        } else {
            coroutineScope.launch {
                fetchForecastData()
            }
        }
    }

    private suspend fun fetchForecastData() = withContext(Dispatchers.IO) {
        try {
            val response = URL("https://api.openweathermap.org/data/2.5/forecast?q=$CITY,$COUNTRY&units=metric&appid=$API").readText()
            weatherDatabase.insertOrUpdateForecastWeather(CITY, COUNTRY, response)
            withContext(Dispatchers.Main) {
                updateUIWithForecastData(response)
            }
        } catch (e: Exception) {
            Log.e("FragmentForecast", "Error fetching forecast data: ${e.message}")
            withContext(Dispatchers.Main) {
                showError("Nastala chyba při načítání počasí. Zkontrolujte připojení k internetu.")
            }
        }
    }

    private fun updateUIWithForecastData(data: String) {
        try {
            val jsonObj = JSONObject(data)
            val list = jsonObj.getJSONArray("list")
            val forecastList = mutableListOf<Forecast>()

            for (i in 0 until list.length()) {
                val forecast = list.getJSONObject(i)
                val main = forecast.getJSONObject("main")
                val weather = forecast.getJSONArray("weather").getJSONObject(0)
                val dateTime = forecast.getLong("dt") * 1000
                val date = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(Date(dateTime))

                val forecastItem = Forecast(
                    date = date,
                    temp = main.getString("temp"),
                    condition = weather.getString("description").capitalize(Locale.getDefault()),
                    iconRes = getWeatherIcon(weather.getString("icon"))
                )
                forecastList.add(forecastItem)
            }

            forecastRecyclerView.layoutManager = LinearLayoutManager(context)
            forecastRecyclerView.adapter = ForecastAdapter(forecastList)

            val lastUpdatedDate = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.ENGLISH).format(Date())
            address.text = "$CITY, $COUNTRY"
            updatedAt.text = "Last Updated: $lastUpdatedDate"

            mainContainer.visibility = View.VISIBLE
            loader.visibility = View.GONE
        } catch (e: Exception) {
            showError("Nastala chyba při zpracování dat.")
        }
    }

    private fun getWeatherIcon(iconCode: String): Int {
        return when (iconCode) {
            "01d" -> R.drawable.ic_sunny
            "02d" -> R.drawable.ic_partly_cloudy
            "03d" -> R.drawable.ic_cloudy
            "04d" -> R.drawable.ic_broken_clouds
            "09d" -> R.drawable.ic_rain
            "10d" -> R.drawable.ic_rain_sun
            "11d" -> R.drawable.ic_thunderstorm
            "13d" -> R.drawable.ic_snow
            "50d" -> R.drawable.ic_mist
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
