package com.example.weatherapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import com.example.weatherapp.workers.NotificationWorker
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.util.*

/**
 * Služba pro pravidelné získávání dat o počasí a zobrazování notifikací.
 */
class NotificationService : Service(), CoroutineScope {

    private var timer: Timer? = null // spouští periodické úlohy
    private val TAG = "NotificationService"
    private var API = BuildConfig.HOME_API
    private var WEATHER_STATION_ID = BuildConfig.STATION_ID
    private val apiUrl = "https://api.weather.com/v2/pws/observations/current?stationId=$WEATHER_STATION_ID&format=json&units=m&apiKey=$API"
    private val INTERVAL_DEBUG = 60 * 1000L // 1 minuta pro debugování
    private val INTERVAL_PROD = 60 * 60 * 1000L // 1 hodina pro produkci
    private var interval = INTERVAL_PROD
    private val job = Job() // práce pro správu coroutine > bylo nutné kvůli odlišným verzím Android OS a jejich oprávněním

    override val coroutineContext
        get() = Dispatchers.Main + job // hlavní vlákno

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Metoda spuštěná při startu služby. Určuje interval a zahajuje periodické získávání dat.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        interval = if (intent?.getBooleanExtra("debug", false) == true) INTERVAL_DEBUG else INTERVAL_PROD
        Log.d(TAG, "Service started with interval: $interval ms")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Using WorkManager for Android 12+")
            startWithWorkManager() // Android 12+ (WorkManager)
        } else {
            Log.d(TAG, "Using startForeground for Android < 12")
            startForeground(1, createNotification("Weather Service", "Fetching weather updates..."))
            startTimer() // pouze časovač pro nižší verze...
        }

        return START_STICKY
    }

    /**
     * Metoda spuštěná při zničení služby. Zastavuje časovač a coroutine.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopTimer() // cancel časovac
        job.cancel() // cancel couroutin
        Log.d(TAG, "Service stopped")
    }

    /**
     * Spustí časovač, který periodicky volá `fetchWeatherData` a zobrazuje notifikace.
     */
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
        }, 0, interval) // okamzite spustim
    }

    /**
     * Zastaví časovač.
     */
    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    /**
     * Načítá data o počasí z API.
     * @return Informace o aktuálním počasí ve formátu `Město, Teplota°C` nebo `null` při chybě.
     */
    private suspend fun fetchWeatherData(): String? {
        return withContext(Dispatchers.IO) { // spustí se v IO vlákně - lepší kvůli síťovým operacím
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

    /**
     * Zobrazuje notifikaci s aktuálními informacemi o počasí.
     * @param title Titulek notifikace.
     * @param message Text notifikace.
     */
    private fun sendNotification(title: String, message: String) {
        val channelId = "WeatherServiceChannel"

        Log.d(TAG, "Sending notification with title: $title and message: $message")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Vytvoření kanálu notifikací (pro Android 8.0 a vyšší)
            val channel = NotificationChannel(channelId, "Weather Updates", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        // vytvorim celkovou notifikaci
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_weather_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(1, notification) // display...
    }

    /**
     * Vytvoří notifikaci pro zobrazení v popředí služby.
     * @param title Titulek notifikace.
     * @param message Text notifikace.
     * @return Objekt `Notification`.
     */
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

    /**
     * Spustí službu pomocí WorkManageru pro zajištění kompatibility s Androidem 12+.
     */
    private fun startWithWorkManager() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .addTag("NOTIFICATION_WORKER_TAG")
            .build()
        WorkManager.getInstance(this).enqueue(workRequest) // jednorazovka - pomoci queue
    }
}
