package com.example.weatherapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherDatabase

class FavoriteCitiesAdapter(
    private val context: Context,
    private val favoriteCities: MutableList<String>,
    private val onCityDeleted: (String) -> Unit,
) : BaseAdapter() {

    private val weatherDatabase: WeatherDatabase = WeatherDatabase(context)

    override fun getCount(): Int = favoriteCities.size

    override fun getItem(position: Int): String = favoriteCities[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.favorite_city_item, parent, false)

        val cityNameTextView = view.findViewById<TextView>(R.id.city_name)
        val deleteIcon = view.findViewById<ImageView>(R.id.delete_icon)

        val cityName = getItem(position)
        cityNameTextView.text = cityName

        deleteIcon.setOnClickListener {
            // Odstraňte město z databáze a seznamu
            weatherDatabase.removeFavoriteCity(cityName, favoriteCities[position].substringAfter(", "))
            favoriteCities.removeAt(position)
            notifyDataSetChanged()

            Toast.makeText(context, "$cityName bylo smazáno z oblíbených", Toast.LENGTH_SHORT).show()

            onCityDeleted(cityName)
        }

        return view
    }
}
