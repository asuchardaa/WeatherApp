package com.example.weatherapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.weatherapp.listeners.OnCitySelectedListener
import com.example.weatherapp.listeners.OnFavoritesUpdatedListener
import com.example.weatherapp.ui.fragment.FragmentForecast
import com.example.weatherapp.ui.fragment.FragmentWeather
import com.example.weatherapp.ui.fragment.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), OnCitySelectedListener, OnFavoritesUpdatedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> {
                    loadFragment(FragmentWeather())
                    true
                }
                R.id.nav_forecast -> {
                    loadFragment(FragmentForecast())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, FragmentWeather())
                .commit()

            bottomNav.selectedItemId = R.id.nav_weather

        }


    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.mainContainer, fragment)
        transaction.commit()
    }

    override fun onCitySelected(city: String, country: String) {
        val fragmentWeather = supportFragmentManager.findFragmentById(R.id.mainContainer) as? FragmentWeather
        fragmentWeather?.onCitySelected(city, country)
    }

    override fun onFavoritesUpdated() {
        // Najděte FragmentWeather a zavolejte updateStarIcon
        val fragment = supportFragmentManager.findFragmentById(R.id.mainContainer)
        if (fragment is FragmentWeather) {
            fragment.updateStarIcon()
        }
    }
}
