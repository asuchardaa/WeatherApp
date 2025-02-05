package com.example.weatherapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Třída WeatherDatabase
 * Přístup k SQLite databázi,
 * Ukládá aktuální počasí, předpovědí a oblíbená města.
 *
 * @constructor instance.
 * @param context otevření/vytvoření databáze.
 */
class WeatherDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeatherData.db"
        private const val DATABASE_VERSION = 6

        // Tabulka pro aktuální počasí
        const val TABLE_CURRENT_WEATHER = "weather"
        const val COLUMN_CITY = "city"
        const val COLUMN_COUNTRY = "country"
        const val COLUMN_DATA = "data"

        // Tabulka pro předpověď počasí
        const val TABLE_FORECAST_WEATHER = "forecast_weather"
        const val COLUMN_FORECAST_DATA = "forecast_data"
        const val COLUMN_TIMESTAMP = "timestamp"

        // Tabulka pro oblíbená města
        const val TABLE_FAVORITE_CITIES = "favorite_cities"
        const val COLUMN_FAVORITE_CITY = "favorite_city"
        const val COLUMN_FAVORITE_COUNTRY = "favorite_country"

    }

    /**
     * Metoda volaná při prvním vytvoření databáze.
     * Vytváří tabulky pro ukládání aktuálního počasí, předpovědí a oblíbených měst.
     */
    override fun onCreate(db: SQLiteDatabase) {
        // Vytvoření tabulky pro aktuální počasí
        val CREATE_CURRENT_WEATHER_TABLE = ("CREATE TABLE $TABLE_CURRENT_WEATHER (" +
                "$COLUMN_CITY TEXT," +
                "$COLUMN_COUNTRY TEXT," +
                "$COLUMN_DATA TEXT," +
                "PRIMARY KEY ($COLUMN_CITY, $COLUMN_COUNTRY))")
        db.execSQL(CREATE_CURRENT_WEATHER_TABLE)

        // Vytvoření tabulky pro předpověď počasí
        val CREATE_FORECAST_WEATHER_TABLE = ("CREATE TABLE $TABLE_FORECAST_WEATHER (" +
                "$COLUMN_CITY TEXT," +
                "$COLUMN_COUNTRY TEXT," +
                "$COLUMN_FORECAST_DATA TEXT," +
                "$COLUMN_TIMESTAMP INTEGER," +
                "PRIMARY KEY ($COLUMN_CITY, $COLUMN_COUNTRY))")
        db.execSQL(CREATE_FORECAST_WEATHER_TABLE)

        // Vytvoření tabulky pro oblíbená města
        val CREATE_FAVORITE_CITIES_TABLE = ("CREATE TABLE $TABLE_FAVORITE_CITIES (" +
                "$COLUMN_FAVORITE_CITY TEXT, " +
                "$COLUMN_FAVORITE_COUNTRY TEXT, " +
                "PRIMARY KEY ($COLUMN_FAVORITE_CITY, $COLUMN_FAVORITE_COUNTRY))")
        db.execSQL(CREATE_FAVORITE_CITIES_TABLE)
    }

    /**
     * Metoda volaná při aktualizaci databáze (např. při změně verze).
     * Odstraňuje staré tabulky a vytváří nové.
     *
     * @param db Instance databáze, která má být aktualizována.
     * @param oldVersion Starší verze databáze.
     * @param newVersion Nová verze databáze.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CURRENT_WEATHER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FORECAST_WEATHER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITE_CITIES")
        onCreate(db)
    }

    /**
     * Vloží nebo aktualizuje aktuální počasí pro zadané město a zemi.
     *
     * @param city Název města.
     * @param country Název země.
     * @param data Data o aktuálním počasí.
     */
    fun insertOrUpdateCurrentWeather(city: String, country: String, data: String) {
        try {
            val db = writableDatabase
            val sql = "INSERT OR REPLACE INTO $TABLE_CURRENT_WEATHER ($COLUMN_CITY, $COLUMN_COUNTRY, $COLUMN_DATA) VALUES (?, ?, ?)"
            db.execSQL(sql, arrayOf(city, country, data))
            Log.d("DatabaseDebug", "Weather data inserted/updated for city: $city, country: $country")
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error saving current weather data: ${e.message}")
        }
    }

    /**
     * Načte aktuální počasí pro dané město a zemi.
     *
     * @param city Název města.
     * @param country Název země.
     * @return Data o aktuálním počasí nebo `null`, pokud nejsou dostupná.
     */
    fun getCurrentWeather(city: String, country: String): String? {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COLUMN_DATA FROM $TABLE_CURRENT_WEATHER WHERE $COLUMN_CITY = ? AND $COLUMN_COUNTRY = ?", arrayOf(city, country))
            cursor.use {
                if (it.moveToFirst()) return it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
            }
            null
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error fetching current weather: ${e.message}")
            null
        }
    }

    /**
     * Vloží nebo aktualizuje předpověď počasí pro zadané město a zemi.
     *
     * @param city Název města.
     * @param country Název země.
     * @param forecastData Data o předpovědi počasí.
     */
    fun insertOrUpdateForecastWeather(city: String, country: String, forecastData: String) {
        try {
            val db = writableDatabase
            val sql = "INSERT OR REPLACE INTO $TABLE_FORECAST_WEATHER ($COLUMN_CITY, $COLUMN_COUNTRY, $COLUMN_FORECAST_DATA) VALUES (?, ?, ?)"
            db.execSQL(sql, arrayOf(city, country, forecastData))
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error saving forecast weather data: ${e.message}")
        }
    }

    /**
     * Načte předpověď počasí pro dané město a zemi.
     *
     * @param city Název města.
     * @param country Název země.
     * @return Data o předpovědi počasí nebo `null`, pokud nejsou dostupná.
     */
    fun getForecastWeather(city: String, country: String): String? {
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000

        return try {
            val db = readableDatabase
            deleteOldForecasts(db)
            val cursor = db.rawQuery(
                "SELECT $COLUMN_FORECAST_DATA FROM $TABLE_FORECAST_WEATHER" +
                        " WHERE $COLUMN_CITY = ? AND $COLUMN_COUNTRY = ? AND $COLUMN_TIMESTAMP > ?",
                arrayOf(city, country, oneDayAgo.toString()))

            // něco jako try/finally v c#, akorát tady jak si hraju s mobilem, tak tu je kurzor a sám se zavře -> lépe se pak uživateli ovládá a píše...
            cursor.use {
                if (it.moveToFirst()) return it.getString(it.getColumnIndexOrThrow(
                    COLUMN_FORECAST_DATA
                ))
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Odstraní staré předpovědi počasí.
     *
     * @param db Instance databáze.
     */
    private fun deleteOldForecasts(db: SQLiteDatabase) {
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        db.execSQL("DELETE FROM $TABLE_FORECAST_WEATHER WHERE $COLUMN_TIMESTAMP < ?", arrayOf(oneDayAgo.toString()))
    }


    /**
     * Vloží nebo aktualizuje oblíbené město.
     *
     * @param city Název města.
     * @param country Název země.
     */
    fun insertOrUpdateFavoriteCity(city: String, country: String) {
        val db = writableDatabase
        val sql = "INSERT OR IGNORE INTO $TABLE_FAVORITE_CITIES ($COLUMN_FAVORITE_CITY, $COLUMN_FAVORITE_COUNTRY) VALUES (?, ?)"
        db.execSQL(sql, arrayOf(city, country))
    }

    /**
     * Odstraní oblíbené město.
     *
     * @param city Název města.
     * @param country Název země.
     */
    fun removeFavoriteCity(city: String, country: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            Log.d("DatabaseDebug", "Attempting to delete city: $city, country: $country") // Ladicí výpis
            val sql = "DELETE FROM $TABLE_FAVORITE_CITIES WHERE $COLUMN_FAVORITE_CITY = ? AND $COLUMN_FAVORITE_COUNTRY = ?"
            val statement = db.compileStatement(sql)
            statement.bindString(1, city.trim())
            statement.bindString(2, country.trim())

            val affectedRows = statement.executeUpdateDelete()
            if (affectedRows > 0) {
                Log.d("DatabaseSuccess", "City $city was removed from favorites")
                db.setTransactionSuccessful()
            } else {
                Log.d("DatabaseError", "No rows deleted for city $city")
            }
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error removing favorite city: ${e.message}")
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    /**
     * Načte všechna oblíbená města.
     *
     * @return Seznam oblíbených měst.
     */
    fun getAllFavoriteCities(): List<String> {
        val db = readableDatabase
        val favoriteCities = mutableListOf<String>()
        val query = "SELECT $COLUMN_FAVORITE_CITY, $COLUMN_FAVORITE_COUNTRY FROM $TABLE_FAVORITE_CITIES"
        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val city = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE_CITY))
            val country = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE_COUNTRY))
            favoriteCities.add("$city, $country")
        }
        cursor.close()
        db.close()
        Log.d("DatabaseContent", "Favorite Cities in DB: $favoriteCities")
        return favoriteCities
    }
}
