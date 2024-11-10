package com.example.weatherapp.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import com.example.weatherapp.R
import com.example.weatherapp.ui.adapter.FavoriteCitiesAdapter
import com.example.weatherapp.listeners.OnCitySelectedListener

class FavoriteCitiesFragment : DialogFragment() {

    private var listener: OnCitySelectedListener? = null
    private lateinit var favoriteCitiesAdapter: FavoriteCitiesAdapter
    private lateinit var favoriteCities: MutableList<String>

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

        favoriteCities = arguments?.getStringArrayList(ARG_FAVORITE_CITIES)?.toMutableList() ?: mutableListOf()

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
        private const val ARG_FAVORITE_CITIES = "favorite_cities"

        fun newInstance(favoriteCities: List<String>): FavoriteCitiesFragment {
            val fragment = FavoriteCitiesFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_FAVORITE_CITIES, ArrayList(favoriteCities))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
