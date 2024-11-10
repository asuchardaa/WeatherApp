package com.example.weatherapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class WeatherDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeatherData.db"
        private const val DATABASE_VERSION = 3

        // Tabulka pro aktuální počasí
        const val TABLE_CURRENT_WEATHER = "weather"
        const val COLUMN_CITY = "city"
        const val COLUMN_COUNTRY = "country"
        const val COLUMN_DATA = "data"

        // Tabulka pro předpověď počasí
        const val TABLE_FORECAST_WEATHER = "forecast_weather"
        const val COLUMN_FORECAST_DATA = "forecast_data"

        // Tabulka pro oblíbená města
        const val TABLE_FAVORITE_CITIES = "favorite_cities"
        const val COLUMN_FAVORITE_CITY = "favorite_city"
    }

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
                "PRIMARY KEY ($COLUMN_CITY, $COLUMN_COUNTRY))")
        db.execSQL(CREATE_FORECAST_WEATHER_TABLE)

        // Vytvoření tabulky pro oblíbená města
        val CREATE_FAVORITE_CITIES_TABLE = ("CREATE TABLE $TABLE_FAVORITE_CITIES (" +
                "$COLUMN_FAVORITE_CITY TEXT PRIMARY KEY)")
        db.execSQL(CREATE_FAVORITE_CITIES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CURRENT_WEATHER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FORECAST_WEATHER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITE_CITIES")
        onCreate(db)
    }

    // Funkce pro vložení nebo aktualizaci aktuálního počasí
    fun insertOrUpdateCurrentWeather(city: String, country: String, data: String) {
        try {
            val db = writableDatabase
            val sql = "INSERT OR REPLACE INTO $TABLE_CURRENT_WEATHER ($COLUMN_CITY, $COLUMN_COUNTRY, $COLUMN_DATA) VALUES (?, ?, ?)"
            db.execSQL(sql, arrayOf(city, country, data))
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error saving current weather data: ${e.message}")
        }
    }

    // Funkce pro načtení aktuálního počasí
    fun getCurrentWeather(city: String, country: String): String? {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COLUMN_DATA FROM $TABLE_CURRENT_WEATHER WHERE $COLUMN_CITY = ? AND $COLUMN_COUNTRY = ?", arrayOf(city, country))
            cursor.use {
                if (it.moveToFirst()) return it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // Funkce pro vložení nebo aktualizaci předpovědi počasí
    fun insertOrUpdateForecastWeather(city: String, country: String, forecastData: String) {
        try {
            val db = writableDatabase
            val sql = "INSERT OR REPLACE INTO $TABLE_FORECAST_WEATHER ($COLUMN_CITY, $COLUMN_COUNTRY, $COLUMN_FORECAST_DATA) VALUES (?, ?, ?)"
            db.execSQL(sql, arrayOf(city, country, forecastData))
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error saving forecast weather data: ${e.message}")
        }
    }

    // Funkce pro načtení předpovědi počasí
    fun getForecastWeather(city: String, country: String): String? {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COLUMN_FORECAST_DATA FROM $TABLE_FORECAST_WEATHER WHERE $COLUMN_CITY = ? AND $COLUMN_COUNTRY = ?", arrayOf(city, country))
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

    // Funkce pro přidání města do oblíbených
    fun insertOrUpdateFavoriteCity(city: String) {
        val db = writableDatabase
        val sql = "INSERT OR IGNORE INTO $TABLE_FAVORITE_CITIES ($COLUMN_FAVORITE_CITY) VALUES (?)"
        db.execSQL(sql, arrayOf(city))
    }

    // Funkce pro odstranění oblíbeného města
    fun removeFavoriteCity(city: String) {
        try {
            val db = writableDatabase
            val sql = "DELETE FROM $TABLE_FAVORITE_CITIES WHERE $COLUMN_FAVORITE_CITY = ?"
            db.execSQL(sql, arrayOf(city))
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error removing favorite city: ${e.message}")
        }
    }

    // Funkce pro načtení oblíbených měst
    fun getFavoriteCities(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_FAVORITE_CITY FROM $TABLE_FAVORITE_CITIES", null)
        val favoriteCities = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                favoriteCities.add(it.getString(it.getColumnIndexOrThrow(COLUMN_FAVORITE_CITY)))
            }
        }
        return favoriteCities
    }


}
