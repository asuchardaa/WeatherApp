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
            "mist" to "Mlhavo",
            "light rain" to "Slabý déšť",
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

        fun translateWeatherCondition(condition: String): String {
            val translations = mapOf(
                "clear sky" to "Jasno",
                "few clouds" to "Polojasno",
                "scattered clouds" to "Převážně oblačno",
                "broken clouds" to "Oblačno",
                "overcast clouds" to "Zataženo",
                "shower rain" to "Přeháňky",
                "rain" to "Déšť",
                "thunderstorm" to "Bouřka",
                "snow" to "Sněžení",
                "mist" to "Mlha",
                "light rain" to "Slabý déšť",
            )
            return translations[condition.lowercase()] ?: condition.capitalize(Locale.getDefault())
        }

        fun translateDate(date: String): String {
            // Překlad anglických dnů a měsíců na české
            val dayTranslations = mapOf(
                "Mon" to "Po",
                "Tue" to "Út",
                "Wed" to "St",
                "Thu" to "Čt",
                "Fri" to "Pá",
                "Sat" to "So",
                "Sun" to "Ne"
            )

            val monthTranslations = mapOf(
                "Jan" to "led",
                "Feb" to "úno",
                "Mar" to "bře",
                "Apr" to "dub",
                "May" to "kvě",
                "Jun" to "čvn",
                "Jul" to "čvc",
                "Aug" to "srp",
                "Sep" to "zář",
                "Oct" to "říj",
                "Nov" to "lis",
                "Dec" to "pro"
            )

            val dateParts = date.split(" ")
            return if (dateParts.size >= 3) {
                // Předpokládáme formát "EEE, d MMM HH:mm"
                val day = dayTranslations[dateParts[0]] ?: dateParts[0] // Přeložit den
                val month = monthTranslations[dateParts[2]] ?: dateParts[2] // Přeložit měsíc
                "$day, ${dateParts[1]} $month ${dateParts[3]}" // Znovu složit datum
            } else {
                // Pokud formát neodpovídá, vrátíme původní řetězec
                date
            }
        }



    }
}
