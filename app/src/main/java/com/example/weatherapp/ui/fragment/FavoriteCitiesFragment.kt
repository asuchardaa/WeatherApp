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

/**
 * Fragment zobrazuje seznam oblíbených měst jako dialog.
 * Umožňuje výběr města nebo jeho odstranění ze seznamu.
 */
class FavoriteCitiesFragment : DialogFragment() {

    private lateinit var favoriteCitiesAdapter: FavoriteCitiesAdapter
    private lateinit var favoriteCities: MutableList<String>
    private lateinit var weatherDatabase: WeatherDatabase

    private var listener: OnCitySelectedListener? = null
    private var favoritesUpdatedListener: OnFavoritesUpdatedListener? = null

    /**
     * Volá se při připojení fragmentu k aktivitě.
     * Zajišťuje, že aktivita implementuje potřebné listenery.
     */
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

    /**
     * Vytváří a inicializuje uživatelské rozhraní fragmentu.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_cities, container, false)

        // Inicializace databáze a načtení oblíbených měst
        weatherDatabase = WeatherDatabase(requireContext())
        favoriteCities = weatherDatabase.getAllFavoriteCities().toMutableList()

        // Nastavení pozadí podle aktuálního motivu

        val sharedPreferencesSettings = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferencesSettings.getString(SettingsFragment.PREF_THEME_KEY, SettingsFragment.THEME_PURPLE)
        val backgroundResource = if (currentTheme == SettingsFragment.THEME_PURPLE) R.drawable.gradient_purple_bg else R.drawable.gradient_green_bg
        view.setBackgroundResource(backgroundResource)

        // Inicializace ListView a jeho adaptéru
        val favoriteCitiesList = view.findViewById<ListView>(R.id.favoriteCitiesListView)
        favoriteCitiesAdapter = FavoriteCitiesAdapter(requireContext(), favoriteCities) { deletedCity ->
            // Odstranění města z databáze a seznamu
            favoriteCities.remove(deletedCity)
            weatherDatabase.removeFavoriteCity(deletedCity.substringBefore(","), deletedCity.substringAfter(", "))
            favoritesUpdatedListener?.onFavoritesUpdated() // oznamuju
            favoriteCitiesAdapter.notifyDataSetChanged() // aktualizuju
        }

        favoriteCitiesList.adapter = favoriteCitiesAdapter

        // Nastavení klikací akce na položky seznamu
        favoriteCitiesList.setOnItemClickListener { _, _, position, _ ->
            val selectedCityAndCountry = favoriteCities[position]
            val city = selectedCityAndCountry.substringBefore(",")
            val country = selectedCityAndCountry.substringAfter(", ")

            listener?.onCitySelected(city, country) // oznamuju
            favoritesUpdatedListener?.onFavoritesUpdated() // aktualizuju
            dismiss()
        }

        return view
    }

    /**
     * Nastavuje rozměry dialogu při jeho zobrazení.
     */
    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        if (window != null) {
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90%, je pekne, kdyz to vyskakuje a neprekryva to celou plochu obrazovky
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT // vysku si nastesti uz nastavuje sam :-)
            window.attributes = params
        }
    }

    /**
     * Volá se při odpojení fragmentu od aktivity.
     * Uvolňuje listenery.
     */
    override fun onDetach() {
        super.onDetach()
        listener = null
        favoritesUpdatedListener = null
    }

    /**
     * Vytváří novou instanci fragmentu.
     */
    companion object {
        fun newInstance(): FavoriteCitiesFragment {
            return FavoriteCitiesFragment()
        }
    }
}
