package com.example.weatherapp.utils

import java.util.*

class WeatherDescriptionTranslator {

    companion object {
        private val translationMap = mapOf(
            "Clear sky" to "jasná obloha",
            "Few clouds" to "málo oblaků",
            "Scattered clouds" to "rozptýlené mraky",
            "Broken clouds" to "zlomené mraky",
            "Shower rain" to "přeháňky",
            "Rain" to "déšť",
            "Thunderstorm" to "bouřka",
            "Snow" to "sníh",
            "Mist" to "mlha"
        )

        /**
         * Překládá anglický popis počasí do češtiny.
         *
         * @param description Anglický popis počasí
         * @return Přeložený popis v češtině nebo původní text, pokud překlad neexistuje
         */
        fun translate(description: String): String {
            return translationMap[description.lowercase(Locale.ENGLISH)] ?: description
        }
    }
}
