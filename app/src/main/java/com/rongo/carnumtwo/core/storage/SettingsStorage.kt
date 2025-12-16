package com.rongo.carnumtwo.core.storage

import android.content.Context
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.config.PrefsKeys

class SettingsStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val x = prefs.getInt(PrefsKeys.GRID_X, GameDefaults.DEFAULT_GRID_X)
        val y = prefs.getInt(PrefsKeys.GRID_Y, GameDefaults.DEFAULT_GRID_Y)
        val tick = prefs.getLong(PrefsKeys.TICK_MS, GameDefaults.DEFAULT_TICK_MS)
        val spawn = prefs.getLong(PrefsKeys.SPAWN_MS, GameDefaults.DEFAULT_SPAWN_MS)
        val lang = prefs.getString(PrefsKeys.LANGUAGE, GameDefaults.DEFAULT_LANGUAGE) ?: GameDefaults.DEFAULT_LANGUAGE
        return AppSettings(x, y, tick, spawn, lang)
    }

    fun saveGrid(x: Int, y: Int) {
        prefs.edit()
            .putInt(PrefsKeys.GRID_X, x)
            .putInt(PrefsKeys.GRID_Y, y)
            .apply()
    }

    fun saveLanguage(language: String) {
        prefs.edit().putString(PrefsKeys.LANGUAGE, language).apply()
    }

    fun saveTiming(tickMs: Long, spawnMs: Long) {
        prefs.edit()
            .putLong(PrefsKeys.TICK_MS, tickMs)
            .putLong(PrefsKeys.SPAWN_MS, spawnMs)
            .apply()
    }
}
