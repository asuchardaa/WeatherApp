package com.example.weatherapp.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.weatherapp.R

class ThemeAdapter(private val context: Context) {

    companion object {
        const val THEME_LIGHT = "Světlý"
        const val THEME_DARK = "Tmavý"
        const val THEME_PURPLE = "Fialový"
        const val THEME_GREEN = "Zelený"
    }

    fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_PURPLE -> applyCustomGradient(R.drawable.gradient_purple_bg)
            THEME_GREEN -> applyCustomGradient(R.drawable.gradient_green_bg)
        }
    }

    private fun applyCustomGradient(resourceId: Int) {
        // Aplikace gradientu na hlavní aktivity
        val window = (context as? androidx.appcompat.app.AppCompatActivity)?.window
        window?.setBackgroundDrawableResource(resourceId)
    }
}
