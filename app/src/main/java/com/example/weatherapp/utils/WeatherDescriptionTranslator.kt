package com.example.weatherapp.utils

import android.util.Log
import java.util.*

class WeatherDescriptionTranslator {

    companion object {
        private val translationMap = mapOf(
            "clear sky" to "Jasná obloha",
            "few clouds" to "Polojasno",
            "scattered clouds" to "Částečně oblačno",
            "broken clouds" to "Převážně oblačno",
            "overcast clouds" to "Zcela zataženo",
            "shower rain" to "Přeháňky",
            "rain" to "Trvalý déšť",
            "thunderstorm" to "Bouřková činnost",
            "snow" to "Sněžení",
            "mist" to "Mlhavo"
        )

        /**
         * Překládá anglický popis počasí do češtiny.
         *
         * @param description Anglický popis počasí
         * @return Přeložený popis v češtině nebo původní text, pokud překlad neexistuje
         */
        fun translate(description: String, locale: Locale = Locale.getDefault()): String {
            // Odstranění mezer na začátku/konce a převedení na malá písmena
            val normalizedDescription = description.trim().lowercase()
            Log.e("WeatherDataParser", "locale: $locale.language")
            return if (locale.language == "cs") {
                translationMap[normalizedDescription] ?: description
            } else {
                description
            }
        }
    }
}
