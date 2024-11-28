package com.example.weatherapp.listeners

/**
 * Rozhraní pro listenery událostí výběru města.
 */
interface OnCitySelectedListener {
    fun onCitySelected(city: String, country: String)
}