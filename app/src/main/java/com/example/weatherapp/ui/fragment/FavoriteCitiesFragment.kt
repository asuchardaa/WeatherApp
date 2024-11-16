package com.example.weatherapp.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherDatabase
import com.example.weatherapp.ui.adapters.FavoriteCitiesAdapter
import com.example.weatherapp.listeners.OnCitySelectedListener
import com.example.weatherapp.listeners.OnFavoritesUpdatedListener

class FavoriteCitiesFragment : DialogFragment() {

    private lateinit var favoriteCitiesAdapter: FavoriteCitiesAdapter
    private lateinit var favoriteCities: MutableList<String>
    private lateinit var weatherDatabase: WeatherDatabase

    private var listener: OnCitySelectedListener? = null
    private var favoritesUpdatedListener: OnFavoritesUpdatedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCitySelectedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnCitySelectedListener")
        }
        if (context is OnFavoritesUpdatedListener) {
            favoritesUpdatedListener = context
        } else {
            throw RuntimeException("$context must implement OnFavoritesUpdatedListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_cities, container, false)

        // Inicializace databáze a načtení oblíbených měst
        weatherDatabase = WeatherDatabase(requireContext())
        favoriteCities = weatherDatabase.getAllFavoriteCities().toMutableList()

        val favoriteCitiesList = view.findViewById<ListView>(R.id.favoriteCitiesListView)
        favoriteCitiesAdapter = FavoriteCitiesAdapter(requireContext(), favoriteCities) { deletedCity ->
            favoriteCities.remove(deletedCity)
            weatherDatabase.removeFavoriteCity(deletedCity.substringBefore(","), deletedCity.substringAfter(", "))
            favoritesUpdatedListener?.onFavoritesUpdated()
            favoriteCitiesAdapter.notifyDataSetChanged()
        }

        favoriteCitiesList.adapter = favoriteCitiesAdapter

        favoriteCitiesList.setOnItemClickListener { _, _, position, _ ->
            val selectedCityAndCountry = favoriteCities[position]
            val city = selectedCityAndCountry.substringBefore(",")
            val country = selectedCityAndCountry.substringAfter(", ")

            listener?.onCitySelected(city, country)
            favoritesUpdatedListener?.onFavoritesUpdated()
            dismiss()
        }

        return view
    }

    companion object {
        fun newInstance(): FavoriteCitiesFragment {
            return FavoriteCitiesFragment()
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        if (window != null) {
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90% šířky obrazovky
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT // Výška přizpůsobená obsahu
            window.attributes = params
        }
    }


    override fun onDetach() {
        super.onDetach()
        listener = null
        favoritesUpdatedListener = null
    }
}
