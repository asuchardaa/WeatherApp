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
import com.example.weatherapp.ui.adapter.FavoriteCitiesAdapter
import com.example.weatherapp.listeners.OnCitySelectedListener

class FavoriteCitiesFragment : DialogFragment() {

    private var listener: OnCitySelectedListener? = null
    private lateinit var favoriteCitiesAdapter: FavoriteCitiesAdapter
    private lateinit var favoriteCities: MutableList<String>
    private lateinit var weatherDatabase: WeatherDatabase

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCitySelectedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnCitySelectedListener")
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
            favoriteCitiesAdapter.notifyDataSetChanged()
        }

        favoriteCitiesList.adapter = favoriteCitiesAdapter

        favoriteCitiesList.setOnItemClickListener { _, _, position, _ ->
            val selectedCityAndCountry = favoriteCities[position]
            val city = selectedCityAndCountry.substringBefore(",")
            val country = selectedCityAndCountry.substringAfter(", ")
            listener?.onCitySelected(city, country)
            dismiss()
        }

        return view
    }

    companion object {
        fun newInstance(): FavoriteCitiesFragment {
            return FavoriteCitiesFragment()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
