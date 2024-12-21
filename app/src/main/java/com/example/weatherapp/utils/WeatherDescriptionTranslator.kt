package com.example.weatherapp.utils

import java.util.*

/**
 * Třída `WeatherDescriptionTranslator` slouží k překladu anglických popisů počasí a dat
 * do češtiny nebo jiných jazyků podle aktuálního nastavení.
 */
class WeatherDescriptionTranslator {

    companion object {
        // Mapa překladů anglických popisů počasí do češtiny - weatherFragment
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
            "moderate rain" to "Střední déšť",
            "thunderstorm with light rain" to "Bouřka s lehkým deštěm",
            "thunderstorm with rain" to "Bouřka s deštěm",
            "thunderstorm with heavy rain" to "Bouřka se silným deštěm",
            "light thunderstorm" to "Slabá bouřka",
            "heavy thunderstorm" to "Silná bouřka",
            "ragged thunderstorm" to "Roztrhaná bouřka",
            "thunderstorm with light drizzle" to "Bouřka s lehkou mrholou",
            "thunderstorm with drizzle" to "Bouřka s mrholou",
            "thunderstorm with heavy drizzle" to "Bouřka se silnou mrholou",
            "light intensity drizzle" to "Slabé mrholení",
            "drizzle" to "Mrholení",
            "heavy intensity drizzle" to "Silné mrholení",
            "light intensity drizzle rain" to "Slabé mrholení s deštěm",
            "drizzle rain" to "Mrholení s deštěm",
            "heavy intensity drizzle rain" to "Silné mrholení s deštěm",
            "shower rain and drizzle" to "Přeháňky a mrholení",
            "heavy shower rain and drizzle" to "Silné přeháňky a mrholení",
            "shower drizzle" to "Mrholivé přeháňky",
            "light snow" to "Slabé sněžení",
            "heavy snow" to "Silné sněžení",
            "light shower sleet" to "Slabé přeháňky s ledovkou",
            "shower sleet" to "Přeháňky s ledovkou",
            "light rain and snow" to "Slabý déšť se sněhem",
            "rain and snow" to "Déšť se sněhem",
            "freezing rain" to "Mrznoucí déšť",
            "fog" to "Hustá mlha",
            "smoke" to "Kouř",
            "haze" to "Oparem",
            "sand/dust whirls" to "Písečné/Práškové víry",
            "volcanic ash" to "Sopečný popel",
            "squalls" to "Poryvy větru",
            "tornado" to "Tornádo"
        )

        /**
         * Překládá popis počasí podle jazyka uživatele.
         *
         * @param description Anglický popis počasí.
         * @param locale Jazykové nastavení (výchozí je aktuální systémový jazyk).
         * @return Přeložený popis, pokud je dostupný, jinak vrací původní popis.
         */
        fun translate(description: String, locale: Locale = Locale.getDefault()): String {
            // Odstranění mezer na začátku/konce a převedení na malá písmena
            val normalizedDescription = description.trim().lowercase() // byla nutná normalizace, protože některé popisy obsahovaly mezery na začátku
            return if (locale.language == "cs") {
                translationMap[normalizedDescription] ?: description
            } else {
                description
            }
        }

        /**
         * Překládá popisy počasí bez ohledu na nastavení `Locale`.
         *
         * @param condition Anglický popis počasí.
         * @return Přeložený popis, pokud je dostupný, jinak vrací původní popis.
         */
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
                "moderate rain" to "Střední déšť",
                "thunderstorm with light rain" to "Bouřka s lehkým deštěm",
                "thunderstorm with rain" to "Bouřka s deštěm",
                "thunderstorm with heavy rain" to "Bouřka se silným deštěm",
                "light thunderstorm" to "Slabá bouřka",
                "heavy thunderstorm" to "Silná bouřka",
                "ragged thunderstorm" to "Roztrhaná bouřka",
                "thunderstorm with light drizzle" to "Bouřka s lehkou mrholou",
                "thunderstorm with drizzle" to "Bouřka s mrholou",
                "thunderstorm with heavy drizzle" to "Bouřka se silnou mrholou",
                "light intensity drizzle" to "Slabé mrholení",
                "drizzle" to "Mrholení",
                "heavy intensity drizzle" to "Silné mrholení",
                "light intensity drizzle rain" to "Slabé mrholení s deštěm",
                "drizzle rain" to "Mrholení s deštěm",
                "heavy intensity drizzle rain" to "Silné mrholení s deštěm",
                "shower rain and drizzle" to "Přeháňky a mrholení",
                "heavy shower rain and drizzle" to "Silné přeháňky a mrholení",
                "shower drizzle" to "Mrholivé přeháňky",
                "light snow" to "Slabé sněžení",
                "heavy snow" to "Silné sněžení",
                "light shower sleet" to "Slabé přeháňky s ledovkou",
                "shower sleet" to "Přeháňky s ledovkou",
                "light rain and snow" to "Slabý déšť se sněhem",
                "rain and snow" to "Déšť se sněhem",
                "freezing rain" to "Mrznoucí déšť",
                "fog" to "Hustá mlha",
                "smoke" to "Kouř",
                "haze" to "Oparem",
                "sand/dust whirls" to "Písečné/Práškové víry",
                "volcanic ash" to "Sopečný popel",
                "squalls" to "Poryvy větru",
                "tornado" to "Tornádo"
            )
            return translations[condition.lowercase()] ?: condition.capitalize(Locale.getDefault())
        }

        /**
         * Vrátí číslo měsíce pro daný formát datumu.
         *
         * @param date Datum ve formátu "EEE, d MMM HH:mm" (např. "Mon, 28 Nov 15:00").
         * @return Řetězec s číslem měsíce (např. "12" pro prosinec).
         */
        fun getMonthNumber(date: String): String {
            return try {
                val formatter = java.text.SimpleDateFormat("EEE, d MMM HH:mm", Locale.ENGLISH)
                val parsedDate = formatter.parse(date)
                val calendar = Calendar.getInstance()
                calendar.time = parsedDate
                (calendar.get(Calendar.MONTH) + 1).toString() // +1, protože Calendar.MONTH začíná od 0
            } catch (e: Exception) {
                "Neznámý"
            }
        }


        /**
         * Překládá anglické dny a měsíce na české v datovém formátu.
         *
         * @param date Datum ve formátu "EEE, d MMM HH:mm" (např. "Mon, 28 Nov 15:00").
         * @return Přeložené datum (např. "Po, 28 lis 15:00").
         */
        fun translateDate(date: String): String {
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
                "Jan" to "1",
                "Feb" to "2",
                "Mar" to "3",
                "Apr" to "4",
                "May" to "5",
                "Jun" to "6",
                "Jul" to "7",
                "Aug" to "8",
                "Sep" to "9",
                "Oct" to "10",
                "Nov" to "11",
                "Dec" to "12"
            )

            val dateParts = date.split(" ")
            return if (dateParts.size >= 3) {
                val month = monthTranslations[dateParts[1]] ?: dateParts[1]
                "${dateParts[0]}.$month ${dateParts[2]}"
            } else {
                date
            }
        }
    }
}
