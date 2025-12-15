// English comments only inside code
package com.rongo.carnumtwo.core.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.localization.LocaleManager

abstract class BaseLocalizedActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val settings = SettingsStorage(newBase).load()
        val wrapped = LocaleManager.wrapContext(newBase, settings.language)
        super.attachBaseContext(wrapped)
    }
}
