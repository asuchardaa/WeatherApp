package com.example.weatherapp.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.weatherapp.MainActivity
import com.example.weatherapp.R
import com.example.weatherapp.services.NotificationService
import com.example.weatherapp.utils.ThemeAdapter
import java.util.*

class SettingsFragment : Fragment() {

    private lateinit var weatherServiceSwitch: SwitchCompat
    private lateinit var animationsSwitch: SwitchCompat
    private lateinit var czechButton: Button
    private lateinit var englishButton: Button
    private lateinit var themeSpinner: Spinner
    private lateinit var themeAdapter: ThemeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        themeAdapter = ThemeAdapter(requireContext())

        weatherServiceSwitch = view.findViewById(R.id.weatherServiceSwitch)
        animationsSwitch = view.findViewById(R.id.animationsSwitch)
        czechButton = view.findViewById(R.id.czechButton)
        englishButton = view.findViewById(R.id.englishButton)
        themeSpinner = view.findViewById(R.id.themeSpinner)

        val sharedPreferences = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

        // Nastavení služby počasí
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

        // Povolení/zakázání animací
        animationsSwitch.isChecked = sharedPreferences.getBoolean("animations_enabled", true)
        animationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference("animations_enabled", isChecked)
        }

        // Výběr jazyka – Čeština
        czechButton.setOnClickListener {
            showRestartDialog("cs")
        }

        // Výběr jazyka – English
        englishButton.setOnClickListener {
            showRestartDialog("en")
        }

        // Nastavení tématu
        val savedTheme = sharedPreferences.getString("selected_theme", ThemeAdapter.THEME_LIGHT)
        themeSpinner.setSelection(
            when (savedTheme) {
                ThemeAdapter.THEME_LIGHT -> 0
                ThemeAdapter.THEME_DARK -> 1
                ThemeAdapter.THEME_PURPLE -> 2
                ThemeAdapter.THEME_GREEN -> 3
                else -> 0
            }
        )

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTheme = parent?.getItemAtPosition(position).toString()
                savePreference("selected_theme", selectedTheme)
                themeAdapter.applyTheme(selectedTheme)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showRestartDialog(language: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.restart_required_title))
            .setMessage(getString(R.string.restart_required_message))
            .setPositiveButton(getString(R.string.restart_now)) { _, _ ->
                updateLocale(language)
                restartApplication()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(requireContext().resources.configuration)
        config.setLocale(locale)
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)

        savePreference("selected_language", language)
        Log.d("SettingsFragment", "Locale updated to: $language")
    }

    private fun restartApplication() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        requireActivity().finish()
        startActivity(intent)
    }

    private fun savePreference(key: String, value: Any) {
        val sharedPreferences = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
            }
            apply()
        }
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(requireContext(), NotificationService::class.java)
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
