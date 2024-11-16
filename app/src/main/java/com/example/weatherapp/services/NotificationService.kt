package com.example.weatherapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*

class NotificationService : Service(), CoroutineScope {

    private var timer: Timer? = null
    private val handler = Handler()
    private val TAG = "NotificationService"
    private var API = BuildConfig.HOME_API
    private var WEATHER_STATION_ID = BuildConfig.STATION_ID
    private val apiUrl = "https://api.weather.com/v2/pws/observations/current?stationId=$WEATHER_STATION_ID&format=json&units=m&apiKey=$API"
    private val INTERVAL_DEBUG = 60 * 1000L // 1 minuta pro debugování
    private val INTERVAL_PROD = 60 * 60 * 1000L // 1 hodina pro produkci
    private var interval = INTERVAL_PROD
    private val job = Job()

    override val coroutineContext
        get() = Dispatchers.Main + job

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        interval = if (intent?.getBooleanExtra("debug", false) == true) INTERVAL_DEBUG else INTERVAL_PROD
        Log.d(TAG, "Service started with interval: $interval ms")

        startForeground(1, createNotification("Weather Service", "Fetching weather updates..."))
        startTimer()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        job.cancel()
        Log.d(TAG, "Service stopped")
    }

    private fun startTimer() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                launch {
                    val weatherData = fetchWeatherData()
                    if (weatherData != null) {
                        sendNotification("Weather Update", "Current weather: $weatherData")
                    } else {
                        Log.e(TAG, "Failed to fetch weather data.")
                    }
                }
            }
        }, 0, interval) // 0 = okamžitý start, interval = opakování
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private suspend fun fetchWeatherData(): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "API URL: $apiUrl")
                val response = URL(apiUrl).readText(Charsets.UTF_8)
                Log.d(TAG, "API Response: $response")

                val weatherJson = JSONObject(response)
                val observations = weatherJson.optJSONArray("observations")
                if (observations != null && observations.length() > 0) {
                    val currentData = observations.getJSONObject(0)
                    val temp = currentData.getJSONObject("metric").optDouble("temp", Double.NaN)
                    val city = currentData.optString("neighborhood", "Unknown location")

                    val weatherInfo = "$city, $temp°C"
                    Log.d(TAG, "Parsed weather info: $weatherInfo")

                    return@withContext weatherInfo
                } else {
                    Log.e(TAG, "No observations found in response")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather data", e)
                return@withContext null
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "WeatherServiceChannel"

        Log.d(TAG, "Sending notification with title: $title and message: $message")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Weather Updates", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_weather_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        Log.d("NotificationService", "Sending notification with title: $title and message: $message")
        notificationManager?.notify(1, notification)
    }

    private fun createNotification(title: String, message: String): Notification {
        val channelId = "WeatherServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Weather Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_weather_notification)
            .build()
    }
}
