package com.example.weatherapp.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.weatherapp.R
import com.example.weatherapp.services.NotificationService

class SettingsFragment : Fragment() {

    private lateinit var weatherServiceSwitch: SwitchCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weatherServiceSwitch = view.findViewById(R.id.weatherServiceSwitch)

        val sharedPreferences = requireActivity().getSharedPreferences("WeatherServicePrefs", Context.MODE_PRIVATE)
        val isServiceEnabled = sharedPreferences.getBoolean("service_enabled", false)
        weatherServiceSwitch.isChecked = isServiceEnabled

        weatherServiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startNotificationService()
            } else {
                stopNotificationService()
            }
            saveServiceState(isChecked)
        }
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(requireContext(), NotificationService::class.java)
        serviceIntent.putExtra("debug", true) // Debug režim
        requireContext().startService(serviceIntent)
    }

    private fun stopNotificationService() {
        val serviceIntent = Intent(requireContext(), NotificationService::class.java)
        requireContext().stopService(serviceIntent)
    }

    private fun saveServiceState(isEnabled: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences("WeatherServicePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("service_enabled", isEnabled).apply()
    }
}
