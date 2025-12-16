package com.rongo.carnumtwo.core.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    fun wrapContext(base: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return base.createConfigurationContext(config)
    }
}
