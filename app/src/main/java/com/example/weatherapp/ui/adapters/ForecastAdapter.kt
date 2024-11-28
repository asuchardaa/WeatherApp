package com.example.weatherapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R

/**
 * Adaptér pro zobrazení seznamu předpovědí počasí v RecyclerView.
 *
 * @property forecastList Seznam objektů předpovědí počasí, které budou zobrazeny.
 */
class ForecastAdapter(private val forecastList: List<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    /**
     * Vytváří novou instanci ViewHolderu pro jednotlivé položky seznamu.
     *
     * @param parent Nadřazený ViewGroup, do kterého bude pohled přidán.
     * @param viewType Typ pohledu (v tomto případě nepoužíváme různé typy).
     * @return Nová instance [ForecastViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        // opet inflate
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    /**
     * Propojuje data s jednotlivými položkami seznamu.
     *
     * @param holder ViewHolder, který reprezentuje položku.
     * @param position Pozice položky v seznamu.
     */
    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecastList[position] // Získání dat pro aktuální pozici (protoze mam seznam, ne jednotlive bloky pro predpovedi)
        holder.forecastDate.text = forecast.date // data předpovědi
        holder.forecastTemp.text = "${forecast.temp}°C" // teplota
        holder.forecastCondition.text = forecast.condition // popis
        holder.forecastIcon.setImageResource(forecast.iconRes)
    }

    /**
     * Vrací počet položek v seznamu.
     *
     * @return Počet položek v seznamu.
     */
    override fun getItemCount(): Int = forecastList.size

    /**
     * Třída ViewHolder pro jednotlivé položky předpovědi počasí.
     *
     * @constructor Inicializuje ViewHolder a najde odkazy na UI prvky v layoutu položky.
     * @param view Pohled reprezentující položku seznamu.
     */
    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val forecastDate: TextView = view.findViewById(R.id.forecastDate)
        val forecastTemp: TextView = view.findViewById(R.id.forecastTemp)
        val forecastCondition: TextView = view.findViewById(R.id.forecastCondition)
        val forecastIcon: ImageView = view.findViewById(R.id.forecastIcon)
    }
}

/**
 * Datová třída reprezentující předpověď počasí.
 *
 * @property date Datum předpovědi (např. "2024-11-28").
 * @property temp Teplota v daný den (např. "15").
 * @property condition Popis počasí (např. "Jasno").
 * @property iconRes ID zdroje ikony reprezentující počasí.
 */
data class Forecast(
    val date: String,
    val temp: String,
    val condition: String,
    val iconRes: Int
)
