package com.example.weatherapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

        // Nastavení navigace
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

        // Oprávnění pro notifikace
        requestNotificationPermission()
    }

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
        val fragment = supportFragmentManager.findFragmentById(R.id.mainContainer)
        if (fragment is FragmentWeather) {
            fragment.updateStarIcon()
        }
    }
}
