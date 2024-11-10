package com.example.weatherapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class FragmentWeather : Fragment()  {
    var CITY = "Prague"
    var COUNTRY = "CZ"
    val API = BuildConfig.API_KEY

    private lateinit var weatherDatabase: WeatherDatabase
    private lateinit var loader: ProgressBar
    private lateinit var mainContainer: RelativeLayout
    private lateinit var errorText: TextView
    private lateinit var favoriteHeartIcon: ImageView
    private lateinit var citySearch : AutoCompleteTextView

    val cityCountryMap = hashMapOf(
        "Prague" to "CZ",
        "Paris" to "FR",
        "Berlin" to "DE",
        "Bratislava" to "SK",
        "London" to "GB",
        "Pardubice" to "CZ",
        "Liberec" to "CZ",
        "Nová Paka" to "CZ"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weather, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weatherDatabase = WeatherDatabase(requireContext())

        loader = view.findViewById(R.id.loader)
        mainContainer = view.findViewById(R.id.mainContainer)
        errorText = view.findViewById(R.id.errorText)
        favoriteHeartIcon = view.findViewById(R.id.favoriteIcon)
        citySearch = view.findViewById(R.id.citySearch)

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        CITY = sharedPreferences.getString("selected_city", "Prague") ?: "Prague"
        COUNTRY = cityCountryMap[CITY] ?: "CZ"

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cityCountryMap.keys.toList()
        )

        citySearch.setAdapter(adapter)
        citySearch.setText(CITY)

        // logika pro změnu města
        citySearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.toString() in cityCountryMap) {

                        CITY = it.toString()
                        COUNTRY = cityCountryMap[CITY] ?: "CZ"
                        Log.d("FragmentWeather", "City selected: $CITY, $COUNTRY")
                        val editor = sharedPreferences.edit()
                        editor.putString("selected_city", CITY)
                        editor.apply()

                        fetchData()

                        citySearch.setText("")
                    }
                }
            }
        })

        // Nastavení klikací logiky na hvězdičku (drawableEnd) v AutoCompleteTextView
        citySearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = citySearch.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val drawableStartX = citySearch.width - citySearch.paddingEnd - drawableEnd.intrinsicWidth
                    if (event.x >= drawableStartX) {
                        // Přidání města do oblíbených
                        if (CITY.isNotEmpty()) {
                            weatherDatabase.insertOrUpdateFavoriteCity(CITY)
                            Toast.makeText(requireContext(), "$CITY přidáno do oblíbených", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // Kliknutí na srdíčko pro zobrazení fragmentu s oblíbenými městy
        favoriteHeartIcon.setOnClickListener {
            val favoriteCities = weatherDatabase.getFavoriteCities()

            val favoriteFragment = FavoriteCitiesFragment.newInstance(favoriteCities)
            favoriteFragment.show(requireActivity().supportFragmentManager, "FavoriteCitiesFragment")
            if (favoriteCities.isEmpty()) {
                Toast.makeText(requireContext(), "Nemáte žádná oblíbená města", Toast.LENGTH_SHORT).show()
            }
        }

        fetchData()
    }

    // Metoda pro přepnutí města
    fun onCitySelected(city: String) {
        Log.d("FragmentWeather", "City selected: $city")

        CITY = city
        COUNTRY = cityCountryMap[CITY] ?: "CZ" // Nastavení země podle vybraného města

        // Uložení vybraného města do SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selected_city", CITY)
        editor.apply()

        // Načtení dat pro nové město
        fetchData()

        citySearch.setText("")
    }

    private fun fetchData() {
        // Kontrola, zda jsou data uložena v databázi
        WeatherTask().execute()
        val cachedData = weatherDatabase.getCurrentWeather(CITY, COUNTRY)
        if (cachedData != null) {
            // Pokud jsou data k dispozici, aktualizuju UI
            updateUIWithWeatherData(cachedData)
        } else {
            // Jinak se volá API
            WeatherTask().execute()
        }
    }

    private fun updateUIWithWeatherData(data: String) {
        if (data != null) {
            try {
                val jsonObj = JSONObject(data)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt: Long = jsonObj.getLong("dt")
                val updatedAtText =
                    "Updated at: " + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(
                        Date(updatedAt * 1000)
                    )
                val temp = main.getString("temp") + "°C"
                val tempMin = "Min Temp: " + main.getString("temp_min") + "°C"
                val tempMax = "Max Temp: " + main.getString("temp_max") + "°C"
                val sunrise: Long = sys.getLong("sunrise")
                val sunset: Long = sys.getLong("sunset")
                val wind = jsonObj.getJSONObject("wind").getString("speed")
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val clouds = jsonObj.getJSONObject("clouds").getString("all")

                // Update UI elements with data
                view?.findViewById<TextView>(R.id.address)?.text = "$CITY, $COUNTRY"
                view?.findViewById<TextView>(R.id.updated_at)?.text = updatedAtText
                view?.findViewById<TextView>(R.id.status)?.text =
                    weather.getString("description").capitalize(Locale.getDefault())
                view?.findViewById<TextView>(R.id.temp)?.text = temp
                view?.findViewById<TextView>(R.id.temp_min)?.text = tempMin
                view?.findViewById<TextView>(R.id.temp_max)?.text = tempMax
                view?.findViewById<TextView>(R.id.sunrise)?.text =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
                view?.findViewById<TextView>(R.id.sunset)?.text =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
                view?.findViewById<TextView>(R.id.wind)?.text = wind
                view?.findViewById<TextView>(R.id.pressure)?.text = pressure
                view?.findViewById<TextView>(R.id.humidity)?.text = humidity
                view?.findViewById<TextView>(R.id.clouds)?.text = clouds

                mainContainer.visibility = View.VISIBLE
                loader.visibility = View.GONE
            } catch (e: Exception) {
                errorText.visibility = View.VISIBLE
                loader.visibility = View.GONE
            }
        } else {
            errorText.visibility = View.VISIBLE
            loader.visibility = View.GONE
        }
    }

    /*private fun updateFavoriteIcon() {
        val isFavorite = weatherDatabase.isFavoriteCity(CITY)
        val drawableRes = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
        favoriteHeartIcon.setImageResource(drawableRes)
    }*/

    @SuppressLint("StaticFieldLeak")
    inner class WeatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            loader.visibility = View.VISIBLE
            mainContainer.visibility = View.GONE
            errorText.visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            var response: String?
            try {
                response =
                    URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY,$COUNTRY&units=metric&appid=$API").readText(
                        Charsets.UTF_8
                    )
            } catch (e: Exception) {
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                weatherDatabase.insertOrUpdateCurrentWeather(CITY, COUNTRY, result)
                updateUIWithWeatherData(result)
            } else {
                errorText.visibility = View.VISIBLE
                loader.visibility = View.GONE
            }
        }
    }
}
