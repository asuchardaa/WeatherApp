package com.example.weatherapp.ui

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class WeatherDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeatherData.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "weather"
        const val COLUMN_CITY = "city"
        const val COLUMN_COUNTRY = "country"
        const val COLUMN_DATA = "data"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = ("CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_CITY TEXT," +
                "$COLUMN_COUNTRY TEXT," +
                "$COLUMN_DATA TEXT," +
                "PRIMARY KEY ($COLUMN_CITY, $COLUMN_COUNTRY))")
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Function to insert or update weather data
    fun insertOrUpdateWeatherData(city: String, country: String, data: String) {
        try {
            val db = writableDatabase
            val sql = "INSERT OR REPLACE INTO $TABLE_NAME ($COLUMN_CITY, $COLUMN_COUNTRY, $COLUMN_DATA) VALUES (?, ?, ?)"
            db.execSQL(sql, arrayOf(city, country, data))
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error saving data: ${e.message}")
        }
    }


    // Function to retrieve weather data
    fun getWeatherData(city: String, country: String): String? {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COLUMN_DATA FROM $TABLE_NAME WHERE $COLUMN_CITY = ? AND $COLUMN_COUNTRY = ?", arrayOf(city, country))
            cursor.use {
                if (it.moveToFirst()) return it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
            }
            return null

        } catch (e: Exception) {
            null
        }

    }
}