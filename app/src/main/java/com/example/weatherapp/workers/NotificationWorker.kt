package com.example.weatherapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import org.json.JSONObject
import java.net.URL

class NotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val TAG = "NotificationWorker"
    private val API = BuildConfig.HOME_API
    private val WEATHER_STATION_ID = BuildConfig.STATION_ID
    private val apiUrl =
        "https://api.weather.com/v2/pws/observations/current?stationId=$WEATHER_STATION_ID&format=json&units=m&apiKey=$API"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting NotificationWorker")

        try {
            val weatherData = fetchWeatherData()
            if (weatherData != null) {
                sendNotification("Weather Update", "Current weather: $weatherData")
            } else {
                Log.e(TAG, "Failed to fetch weather data.")
                return Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in NotificationWorker: ${e.message}", e)
            return Result.failure()
        }

        return Result.success()
    }

    private suspend fun fetchWeatherData(): String? {
        return try {
            val response = URL(apiUrl).readText(Charsets.UTF_8)
            val weatherJson = JSONObject(response)
            val observations = weatherJson.optJSONArray("observations")
            if (observations != null && observations.length() > 0) {
                val currentData = observations.getJSONObject(0)
                val temp = currentData.getJSONObject("metric").optDouble("temp", Double.NaN)
                val city = currentData.optString("neighborhood", "Unknown location")
                "$city, $temp°C"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather data", e)
            null
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "WeatherServiceChannel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Weather Updates", NotificationManager.IMPORTANCE_DEFAULT)
            applicationContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_weather_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(1, notification)
    }
}
