package com.example.weatherapp.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherDatabase

/**
 * Adaptér pro zobrazení seznamu oblíbených měst v ListView.
 *
 * @property context Kontext aplikace pro práci s UI.
 * @property favoriteCities Seznam oblíbených měst ve formátu "Město, Země".
 * @property onCityDeleted Callback, který se zavolá po smazání města.
 */
class FavoriteCitiesAdapter(
    private val context: Context,
    private val favoriteCities: MutableList<String>,
    private val onCityDeleted: (String) -> Unit, // tohle musí být takhle předáváno, protože jinak by se to nepromitalo i na ostatnich fragmentech
) : BaseAdapter() {

    private val weatherDatabase: WeatherDatabase = WeatherDatabase(context)

    /**
     * Vrací počet položek v seznamu oblíbených měst.
     */
    override fun getCount(): Int = favoriteCities.size

    /**
     * Vrací položku (název města) na dané pozici v seznamu.
     */
    override fun getItem(position: Int): String = favoriteCities[position]

    /**
     * Vrací ID položky (v tomto případě její pozici).
     */
    override fun getItemId(position: Int): Long = position.toLong()

    /**
     * Vytváří nebo recykluje pohled (View) pro každou položku v seznamu.
     *
     * @param position Pozice položky v seznamu.
     * @param convertView Recyklovaný pohled (pokud existuje), jinak `null`.
     * @param parent Nadřazená ViewGroup pro tento pohled.
     * @return Vytvořený nebo upravený pohled pro danou položku.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Recyklace pohledu, pokud je k dispozici, jinak inflace nového layoutu -.> nutno, protože se to volá vždy, když se zobrazí fragment a někdy docházelo k chybám -> byl nutný debug
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.favorite_city_item, parent, false)

        val cityNameTextView = view.findViewById<TextView>(R.id.city_name)
        val deleteIcon = view.findViewById<ImageView>(R.id.delete_icon)


        val cityName = getItem(position)
        cityNameTextView.text = cityName

        // tady to bylo nutny ocistit, protoze to je spojeny s databazi a kdyz se to neocisti, tak se to neaktualizuje
        deleteIcon.setOnClickListener {
            val cityAndCountry = favoriteCities[position]
            val city = cityAndCountry.substringBefore(", ").trim()
            val country = cityAndCountry.substringAfter(", ").trim()

            // smazání města z databáze
            weatherDatabase.removeFavoriteCity(city, country)
            // smazání města z listu oblíbených měst
            favoriteCities.removeAt(position)
            // aktualizuju seznam měst
            notifyDataSetChanged()

            Toast.makeText(context, "$city bylo smazáno z oblíbených", Toast.LENGTH_SHORT).show()
            Log.d("FavoriteCitiesAdapter", "City $city, $country deleted")

            // pure callback, ktery se zavola po smazani mesta :-)
            onCityDeleted(city)
        }
        return view
    }
}
