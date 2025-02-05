package com.example.weatherapp

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.weatherapp.listeners.OnCitySelectedListener
import com.example.weatherapp.listeners.OnFavoritesUpdatedListener
import com.example.weatherapp.ui.fragment.ForecastFragment
import com.example.weatherapp.ui.fragment.WeatherFragment
import com.example.weatherapp.ui.fragment.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

/**
 * Hlavní aktivita aplikace, která spravuje navigaci a fragmenty.
 * Implementuje rozhraní `OnCitySelectedListener` a `OnFavoritesUpdatedListener`.
 */
class MainActivity : AppCompatActivity(), OnCitySelectedListener, OnFavoritesUpdatedListener {

    /**
     * Přetížená metoda `attachBaseContext`, která nastavuje lokalizaci aplikace.
     * Na základě preferencí uživatele nastaví jazyk aplikace (čeština/angličtina).
     */
    override fun attachBaseContext(newBase: Context) {
        // Inicializace `Locale` z preferencí
        val sharedPreferences = newBase.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString(SettingsFragment.PREF_LANGUAGE_KEY, Locale.getDefault().language)
        val locale = Locale(language ?: Locale.getDefault().language)

        // Aplikace nového nastavení jazyka
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    /**
     * Aktualizuje vzhled fragmentů na základě vybraného tématu.
     *
     * @param theme Vybrané téma (fialové/zelené).
     */
    fun updateThemeForFragments(theme: String) {
        val fragmentContainer = findViewById<View>(R.id.mainContainer)
        val backgroundResource = if (theme == SettingsFragment.THEME_PURPLE) R.drawable.gradient_purple_bg else R.drawable.gradient_green_bg
        fragmentContainer.setBackgroundResource(backgroundResource)
    }

    /**
     * Vytváří aktivitu, nastavuje vzhled a navigaci.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Načtení uloženého tématu
        val sharedPreferences = getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val savedTheme = sharedPreferences.getString(SettingsFragment.PREF_THEME_KEY, SettingsFragment.THEME_PURPLE)
        updateThemeForFragments(savedTheme!!)

        // Nastavení navigace
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> {
                    loadFragment(WeatherFragment())
                    true
                }
                R.id.nav_forecast -> {
                    loadFragment(ForecastFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // Defaultní fragment při prvním načtení
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, WeatherFragment())
                .commit()

            bottomNav.selectedItemId = R.id.nav_weather
        }

        // Požádání o oprávnění pro notifikace
        requestNotificationPermission()
    }

    /**
     * Požádá uživatele o oprávnění k zobrazování notifikací (pro Android 13+).
     */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    /**
     * Zpracovává výsledky žádostí o oprávnění.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "POST_NOTIFICATIONS permission granted.")
            } else {
                Log.d("Permission", "POST_NOTIFICATIONS permission denied.")
            }
        }
    }

    /**
     * Načte a zobrazuje vybraný fragment.
     *
     * @param fragment Fragment, který se má zobrazit.
     */
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.mainContainer, fragment)
        transaction.commit()
    }

    /**
     * Reaguje na výběr města.
     * Předává informace o vybraném městě do `WeatherFragment`.
     */
    override fun onCitySelected(city: String, country: String) {
        val fragmentWeather = supportFragmentManager.findFragmentById(R.id.mainContainer) as? WeatherFragment
        fragmentWeather?.onCitySelected(city, country)
    }

    /**
     * Aktualizuje stav oblíbených položek.
     * Zajišťuje aktualizaci ikon ve `WeatherFragment`.
     */
    override fun onFavoritesUpdated() {
        val fragment = supportFragmentManager.findFragmentById(R.id.mainContainer)
        if (fragment is WeatherFragment) {
            fragment.updateStarIcon()
        }
    }
}
