package com.example.weatherapp.ui.fragment

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
import com.example.weatherapp.data.WeatherDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class FragmentWeather : Fragment() {
    var CITY = "Prague"
    var COUNTRY = "CZ"
    val API = BuildConfig.API_KEY

    private lateinit var weatherDatabase: WeatherDatabase
    private lateinit var loader: ProgressBar
    private lateinit var mainContainer: RelativeLayout
    private lateinit var errorText: TextView
    private lateinit var favoriteHeartIcon: ImageView
    private lateinit var citySearch: AutoCompleteTextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
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
        sharedPreferences.edit().remove("city").apply()
        sharedPreferences.edit().remove("country").apply()
        CITY = sharedPreferences.getString("selected_city", "Prague") ?: "Prague"
        COUNTRY = sharedPreferences.getString("selected_country", "CZ") ?: "CZ"
        citySearch.setText(CITY)

        // logika pro změnu města
        citySearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.length > 2) {
                        fetchCitySuggestions(it.toString())
                        //citySearch.setText("")
                    }
                }
            }
        })

        citySearch.setOnItemClickListener { parent, _, position, _ ->
            val selectedCity = parent.getItemAtPosition(position) as String
            CITY = selectedCity.substringBefore(",") // město
            COUNTRY = selectedCity.substringAfter(", ").take(2) // země (2 chary)

            // Uložení vybraného města do SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("selected_city", CITY)
            editor.putString("selected_country", COUNTRY)
            editor.apply()

            fetchData()
            citySearch.clearFocus()
        }

        // diky gpt, drawableEnd bych nikdy nezvladnul :D
        // pocitani souradnic, husty
        citySearch.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = citySearch.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val drawableStartX = citySearch.width - citySearch.paddingEnd - drawableEnd.intrinsicWidth
                    if (event.x >= drawableStartX) {
                        // Přidání města do oblíbených
                        if (CITY.isNotEmpty()) {
                            weatherDatabase.insertOrUpdateFavoriteCity(CITY, COUNTRY)
                            Toast.makeText(requireContext(), "$CITY přidáno do oblíbených", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        favoriteHeartIcon.setOnClickListener {
            val favoriteCities = weatherDatabase.getFavoriteCities()

            val favoriteFragment = FavoriteCitiesFragment.newInstance(favoriteCities)
            favoriteFragment.show(requireActivity().supportFragmentManager, "FavoriteCitiesFragment")
            if (favoriteCities.isEmpty()) {
                Toast.makeText(requireContext(), "Nemáte žádná oblíbená města", Toast.LENGTH_SHORT).show()
            }
        }

        val homeIcon = view.findViewById<ImageView>(R.id.homeIcon)
        homeIcon.setOnClickListener {
            val homeFragmentDialog = HomeFragmentDialog()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, homeFragmentDialog)
                .addToBackStack(null)
                .commit()
        }



        fetchData()
    }

    fun onCitySelected(city: String, country: String) {
        CITY = city
        COUNTRY = country

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selected_city", CITY)
        editor.putString("selected_country", COUNTRY)
        editor.apply()

        fetchData()
        citySearch.setText("")
    }


    private fun fetchCitySuggestions(query: String) {
        AsyncTask.execute {
            // URL API s požadavkem pouze na název města a s limitem 5 výsledků
            val apiUrl = "https://api.openweathermap.org/geo/1.0/direct?q=$query&limit=5&appid=$API"
            try {
                val response = URL(apiUrl).readText()
                val cities = parseCitySuggestions(response)
                requireActivity().runOnUiThread {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities)
                    citySearch.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("FragmentWeather", "Error fetching city suggestions", e)
            }
        }
    }

    private fun parseCitySuggestions(response: String): List<String> {
        val cities = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val cityObject = jsonArray.getJSONObject(i)
                val name = cityObject.getString("name")
                val country = cityObject.getString("country")
                cities.add("$name, $country")
            }
        } catch (e: Exception) {
            Log.e("FragmentWeather", "Error parsing city suggestions", e)
        }
        return cities
    }

    private fun fetchData() {
        WeatherTask().execute()
        val cachedData = weatherDatabase.getCurrentWeather(CITY, COUNTRY)
        if (cachedData != null) {
            updateUIWithWeatherData(cachedData)
        } else {
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
