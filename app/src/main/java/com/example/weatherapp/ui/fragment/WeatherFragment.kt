package com.example.weatherapp.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import com.google.android.gms.location.LocationServices
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.weatherapp.BuildConfig
import android.Manifest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherDatabase
import com.example.weatherapp.listeners.OnFavoritesUpdatedListener
import com.example.weatherapp.utils.WeatherDataParser
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


@Suppress("DEPRECATION")
class WeatherFragment : Fragment(), OnFavoritesUpdatedListener {
    var CITY = "Prague"
    var COUNTRY = "CZ"
    val API = BuildConfig.API_KEY

    private lateinit var weatherDatabase: WeatherDatabase
    private lateinit var loader: ProgressBar
    private lateinit var mainContainer: RelativeLayout
    private lateinit var errorText: TextView
    private lateinit var favoriteHeartIcon: ImageView
    private lateinit var citySearch: AutoCompleteTextView
    private lateinit var locationIcon: ImageView
    private lateinit var locationManager: LocationManager
    private val language = SettingsFragment.selectedLanguage

    private val gpsTimeoutHandler = Handler(Looper.getMainLooper())
    private var gpsTimeoutRunnable: Runnable? = null
    private val locationPermissionCode = 2


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_weather, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferencesSettings = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferencesSettings.getString(SettingsFragment.PREF_THEME_KEY, SettingsFragment.THEME_PURPLE)
        val backgroundResource = if (currentTheme == SettingsFragment.THEME_PURPLE) R.drawable.gradient_purple_bg else R.drawable.gradient_green_bg
        view.setBackgroundResource(backgroundResource)

        weatherDatabase = WeatherDatabase(requireContext())

        loader = view.findViewById(R.id.loader)
        mainContainer = view.findViewById(R.id.mainContainer)
        errorText = view.findViewById(R.id.errorText)
        favoriteHeartIcon = view.findViewById(R.id.favoriteIcon)
        citySearch = view.findViewById(R.id.citySearch)
        locationIcon = view.findViewById(R.id.locationIcon)
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        citySearch.hint = getString(R.string.citySearch)
        locationIcon.contentDescription = getString(R.string.locationIcon)
        errorText.text = getString(R.string.errorText)
        favoriteHeartIcon.contentDescription = getString(R.string.favoriteIcon)

        view?.findViewById<TextView>(R.id.address)?.text = getString(R.string.address)
        view?.findViewById<TextView>(R.id.updated_at)?.text = getString(R.string.updated_at)
        view?.findViewById<TextView>(R.id.status)?.text = getString(R.string.status)
        view?.findViewById<TextView>(R.id.temp)?.text = getString(R.string.temp)
        view?.findViewById<TextView>(R.id.temp_min)?.text = getString(R.string.min_temp)
        view?.findViewById<TextView>(R.id.temp_max)?.text = getString(R.string.max_temp)
        view?.findViewById<TextView>(R.id.sunrise)?.text = getString(R.string.sunrise)
        view?.findViewById<TextView>(R.id.sunset)?.text = getString(R.string.sunset)
        view?.findViewById<TextView>(R.id.wind)?.text = getString(R.string.wind)
        view?.findViewById<TextView>(R.id.pressure)?.text = getString(R.string.pressure)
        view?.findViewById<TextView>(R.id.humidity)?.text = getString(R.string.humidity)
        view?.findViewById<TextView>(R.id.clouds)?.text = getString(R.string.clouds)

        locationIcon.setOnClickListener {
            getCurrentLocation()
        }

        updateGpsIcon()

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("city").apply()
        sharedPreferences.edit().remove("country").apply()
        CITY = sharedPreferences.getString("selected_city", "Prague") ?: "Prague"
        COUNTRY = sharedPreferences.getString("selected_country", "CZ") ?: "CZ"
        citySearch.setText(CITY)

        updateStarIcon()

        // logika pro změnu města
        citySearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.length > 2) {
                        fetchCitySuggestions(it.toString())
                        //citySearch.setText("")
                        updateStarIcon()
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
            citySearch.setText("")

            updateStarIcon()
        }

        // diky gpt, drawableEnd bych nikdy nezvladnul :D
        // pocitani souradnic, husty
        citySearch.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = citySearch.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val drawableStartX = citySearch.width - citySearch.paddingEnd - drawableEnd.intrinsicWidth
                    if (event.x >= drawableStartX) {
                        // Přidání nebo odstranění města z oblíbených
                        val isFavorite = isCityInFavorites(CITY, COUNTRY)
                        // Přidání města do oblíbených
                        if (CITY.isNotEmpty()) {
                            weatherDatabase.insertOrUpdateFavoriteCity(CITY, COUNTRY)
                            Toast.makeText(requireContext(), getString(R.string.favorite_added), Toast.LENGTH_SHORT).show()
                            updateStarIcon()
                        }
                        if (isFavorite) {
                            updateStarIcon()
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        favoriteHeartIcon.setOnClickListener {
            val favoriteCities = weatherDatabase.getAllFavoriteCities()

            val favoriteFragment = FavoriteCitiesFragment.newInstance()
            favoriteFragment.show(requireActivity().supportFragmentManager, "FavoriteCitiesFragment")
            if (favoriteCities.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.favorites_empty), Toast.LENGTH_SHORT).show()
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
        updateStarIcon()
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
                Log.e("WeatherFragment", "Error fetching city suggestions", e)
            }
        }
    }

    private fun parseCitySuggestions(response: String): List<String> {
        val cities = mutableSetOf<String>()
        try {
            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val cityObject = jsonArray.getJSONObject(i)
                val name = cityObject.getString("name").trim()
                val country = cityObject.getString("country").trim()
                cities.add("$name, $country")
            }
        } catch (e: Exception) {
            Log.e("WeatherFragment", "Error parsing city suggestions", e)
        }
        return cities.toList()
    }

    private fun fetchData() {
        val cachedData = weatherDatabase.getCurrentWeather(CITY, COUNTRY)

        if (!isInternetAvailable()) {
            if (cachedData != null) {
                // Zobrazení uložených dat
                updateUIWithWeatherData(cachedData)
                Toast.makeText(requireContext(), getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            } else {
                // Žádná data nejsou k dispozici
                errorText.visibility = View.VISIBLE
                loader.visibility = View.GONE
                Toast.makeText(requireContext(), getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            }
        } else {
            if (cachedData != null) {
                // Nejprve zobrazíme uložená data
                updateUIWithWeatherData(cachedData)
            }
            // Poté stáhneme nová data
            WeatherTask().execute()
        }
    }



    private fun getCurrentLocation() {
        Log.d("GPS_FW", "Starting getCurrentLocation()")

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("GPS_FW", "Permission not granted. Requesting permission...")

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            Log.d("GPS_FW", "Permission granted. Starting GPS updates...")

            locationIcon.setImageResource(R.drawable.gps_not_fixed)
            startGpsLoaderAnimation()

            gpsTimeoutRunnable = Runnable {
                Log.d("GPS_FW", "GPS timeout reached. Stopping location updates.")
                stopGpsLoaderAnimation()
                locationManager.removeUpdates(locationListener)

                Log.d("GPS_FW", "Attempting to fetch last known location.")
                fetchLastKnownLocation()
            }
            gpsTimeoutHandler.postDelayed(gpsTimeoutRunnable!!, 20000)

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // Aktualizace každou sekundu
                1f    // Minimální změna polohy 1 metr
            ) { location ->
                Log.d("GPS_FW", "Location update received: Lat=${location.latitude}, Lon=${location.longitude}")
                gpsTimeoutHandler.removeCallbacks(gpsTimeoutRunnable!!)
                stopGpsLoaderAnimation()
                fetchNearestCityFromLocation(location)
            }
        }
    }

    private fun fetchLastKnownLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Oprávnění uděleno, pokračujeme v získání poslední známé polohy
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("GPS_FW", "Last known location found: Lat=${location.latitude}, Lon=${location.longitude}")
                    fetchNearestCityFromLocation(location)
                } else {
                    Log.d("GPS_FW", "No last known location available.")
                    showManualCitySelectionDialog()
                }
            }.addOnFailureListener { e ->
                Log.e("GPS_FW", "Failed to get last known location: ${e.message}")
                showManualCitySelectionDialog()
            }
        } else {
            // Oprávnění nebylo uděleno, požádáme o něj
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    private val locationListener = LocationListener { location ->
        Log.d("GPS_FW", "Location update received: Lat=${location.latitude}, Lon=${location.longitude}")

        locationIcon.setImageResource(R.drawable.gps_fixed)
        gpsTimeoutHandler.removeCallbacks(gpsTimeoutRunnable!!)
        stopGpsLoaderAnimation()
        fetchNearestCityFromLocation(location)
    }

    private fun fetchNearestCityFromLocation(location: Location) {
        Log.d("GPS_FW", "Fetching nearest city for location: Lat=${location.latitude}, Lon=${location.longitude}")

        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = try {
            geocoder.getFromLocation(location.latitude, location.longitude, 5)
        } catch (e: Exception) {
            Log.e("GPS_FW", "Error using Geocoder: ${e.message}")
            null
        }

        if (addresses != null && addresses.isNotEmpty()) {
            Log.d("GPS_FW", "Geocoder returned ${addresses.size} results.")

            val cityList = addresses.mapNotNull { address ->
                val cityName = address.locality ?: "Unknown"
                val countryName = address.countryCode ?: "Unknown"
                if (cityName != "Unknown") "$cityName, $countryName" else null
            }

            if (cityList.isNotEmpty()) {
                Log.d("GPS_FW", "Valid cities found: $cityList")
                showCitySelectionDialog(cityList)
            } else {
                Log.d("GPS_FW", "No valid cities found in Geocoder results.")
                showManualCitySelectionDialog()
            }
        } else {
            Log.d("GPS_FW", "Geocoder returned no results.")
            showManualCitySelectionDialog()
        }
    }

    private fun showManualCitySelectionDialog() {
        Log.d("GPS_FW", "Displaying manual city selection dialog.")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select City")
        builder.setMessage("We couldn't detect your location. Please enter a city manually.")

        val input = EditText(requireContext())
        input.hint = "Enter city, country"
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val selectedCity = input.text.toString()
            Log.d("GPS_FW", "Manual city input: $selectedCity")

            if (selectedCity.contains(",")) {
                CITY = selectedCity.substringBefore(",").trim()
                COUNTRY = selectedCity.substringAfter(",").trim()

                Log.d("GPS_FW", "Parsed city: $CITY, country: $COUNTRY")

                val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("selected_city", CITY)
                    putString("selected_country", COUNTRY)
                    apply()
                }

                fetchData()
            } else {
                Log.d("GPS_FW", "Invalid city input. Prompting user again.")
                Toast.makeText(requireContext(), getString(R.string.invalid_city), Toast.LENGTH_SHORT).show()
                restorePreviousCity()
            }
        }

        builder.setNegativeButton("Cancel") { _, _ ->
            Log.d("GPS_FW", "Manual city selection canceled.")
            restorePreviousCity()
        }
        builder.show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun showCitySelectionDialog(cityList: List<String>) {
        Log.d("GPS_FW", "Displaying city selection dialog with options: $cityList")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select a City")
        builder.setItems(cityList.toTypedArray()) { _, which ->
            val selectedCity = cityList[which]
            Log.d("GPS_FW", "User selected city: $selectedCity")

            CITY = selectedCity.substringBefore(",").trim()
            COUNTRY = selectedCity.substringAfter(",").trim()

            Log.d("GPS_FW", "Parsed city: $CITY, country: $COUNTRY")

            val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("selected_city", CITY)
                putString("selected_country", COUNTRY)
                apply()
            }

            fetchData()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            Log.d("GPS_FW", "City selection canceled.")
            restorePreviousCity()
        }
        builder.show()
    }

    private fun isCityInFavorites(city: String, country: String): Boolean {
        val favoriteCities = weatherDatabase.getAllFavoriteCities()
        val cityWithCountry = "$city, $country"
        return favoriteCities.contains(cityWithCountry)
    }

    fun updateStarIcon() {
        val isFavorite = isCityInFavorites(CITY, COUNTRY)
        val starIcon = if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star
        citySearch.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, starIcon, 0)
    }

    private fun restorePreviousCity() {
        Log.d("GPS_FW", "Restoring previous city.")

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        CITY = sharedPreferences.getString("selected_city", "Prague") ?: "Prague"
        COUNTRY = sharedPreferences.getString("selected_country", "CZ") ?: "CZ"
        fetchData()
    }

    private fun updateGpsIcon() {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d("GPS_FW", "Updating GPS icon. GPS Enabled: $isGpsEnabled")
        locationIcon.setImageResource(
            if (isGpsEnabled) R.drawable.gps_not_fixed else R.drawable.gps_off
        )
    }

    private fun startGpsLoaderAnimation() {
        Log.d("GPS_FW", "Starting GPS loader animation.")

        val gpsLoaderIcon = activity?.findViewById<ImageView>(R.id.gps_loader_icon)
        gpsLoaderIcon?.visibility = View.VISIBLE
        val rotateAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_gps)
        gpsLoaderIcon?.startAnimation(rotateAnimation)
        showDarkOverlay()
    }

    private fun stopGpsLoaderAnimation() {
        Log.d("GPS_FW", "Stopping GPS loader animation.")

        val gpsLoaderIcon = activity?.findViewById<ImageView>(R.id.gps_loader_icon)
        gpsLoaderIcon?.clearAnimation()
        gpsLoaderIcon?.visibility = View.GONE
        hideDarkOverlay()
    }

    private fun showDarkOverlay() {
        val darkOverlay = activity?.findViewById<View>(R.id.dark_overlay)
        darkOverlay?.visibility = View.VISIBLE
    }

    private fun hideDarkOverlay() {
        val darkOverlay = activity?.findViewById<View>(R.id.dark_overlay)
        darkOverlay?.visibility = View.GONE
    }

    override fun onFavoritesUpdated() {
        updateStarIcon()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), getString(R.string.no_permission), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateGpsIcon()
        updateStarIcon()
        fetchData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(locationListener)
        gpsTimeoutRunnable?.let {
            gpsTimeoutHandler.removeCallbacks(it)
        }
    }

    private fun updateUIWithWeatherData(data: String) {
        if (data.isNotEmpty()) {
            try {
                val jsonObj = JSONObject(data)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt: Long = jsonObj.getLong("dt")
                val parser = WeatherDataParser(data)
                Log.d("WeatherDataParser", "Current language: $language")

                if (language == "en") {
                    val updatedAtText = getString(R.string.updated_at) + ": " + SimpleDateFormat("dd/MM/yyyy hh:mm", Locale.ENGLISH).format(Date(updatedAt * 1000))
                    val temp = Math.round(main.getDouble("temp")).toString() + "°C"
                    val tempMin = getString(R.string.min_temp) + ": " + main.getString("temp_min") + "°C"
                    val tempMax = getString(R.string.max_temp) + ": " + main.getString("temp_max") + "°C"
                    val sunrise: Long = sys.getLong("sunrise")
                    val sunset: Long = sys.getLong("sunset")
                    val wind = jsonObj.getJSONObject("wind").getString("speed")
                    val pressure = main.getString("pressure")
                    val humidity = main.getString("humidity")
                    val clouds = jsonObj.getJSONObject("clouds").getString("all")

                    // Update UI elements
                    view?.findViewById<TextView>(R.id.address)?.text = "$CITY, $COUNTRY"
                    view?.findViewById<TextView>(R.id.updated_at)?.text = updatedAtText
                    view?.findViewById<TextView>(R.id.status)?.text = parser.getWeatherDescription(language)
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

                } else if (language == "cs") {
                    val parser = WeatherDataParser(data)
                    view?.findViewById<TextView>(R.id.address)?.text = "$CITY, $COUNTRY"
                    view?.findViewById<TextView>(R.id.updated_at)?.text = parser.getUpdatedAtText(requireContext())
                    view?.findViewById<TextView>(R.id.status)?.text = parser.getWeatherDescription(language)
                    view?.findViewById<TextView>(R.id.temp)?.text = parser.getTemperature()
                    view?.findViewById<TextView>(R.id.temp_min)?.text = parser.getMinTemperature(requireContext())
                    view?.findViewById<TextView>(R.id.temp_max)?.text = parser.getMaxTemperature(requireContext())
                    view?.findViewById<TextView>(R.id.sunrise)?.text = parser.getSunrise()
                    view?.findViewById<TextView>(R.id.sunset)?.text = parser.getSunset()
                    view?.findViewById<TextView>(R.id.wind)?.text = parser.getWind()
                    view?.findViewById<TextView>(R.id.pressure)?.text = parser.getPressure()
                    view?.findViewById<TextView>(R.id.humidity)?.text = parser.getHumidity()
                    view?.findViewById<TextView>(R.id.clouds)?.text = parser.getClouds()
                }
                mainContainer.visibility = View.VISIBLE
                loader.visibility = View.GONE
            } catch (e: Exception) {
                errorText.visibility = View.VISIBLE
                loader.visibility = View.GONE
                Log.e("WeatherFragment", "Error parsing weather data: ${e.message}")
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
            return try {
                val apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=$CITY,$COUNTRY&units=metric&appid=$API"
                val response = URL(apiUrl).readText(Charsets.UTF_8)

                weatherDatabase.insertOrUpdateCurrentWeather(CITY, COUNTRY, response)
                response
            } catch (e: Exception) {
                Log.e("WeatherTask", "Error fetching weather data: ${e.message}")
                null
            }
        }

        override fun onPostExecute(result: String?) {
            if (result != null) {
                // Aktualizovat UI s novými daty
                updateUIWithWeatherData(result)
            } else {
                // Pokud selže stahování dat, načíst uložená data
                val cachedData = weatherDatabase.getCurrentWeather(CITY, COUNTRY)
                if (cachedData != null) {
                    updateUIWithWeatherData(cachedData)
                    Toast.makeText(requireContext(), getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
                } else {
                    errorText.visibility = View.VISIBLE
                    loader.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                }
            }
            loader.visibility = View.GONE
        }
    }
}