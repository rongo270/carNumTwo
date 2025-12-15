// English comments only inside code
package com.rongo.carnumtwo.core.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.rongo.carnumtwo.core.localization.LocaleManager
import com.rongo.carnumtwo.core.storage.SettingsStorage

abstract class BaseLocalizedActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsStorage(newBase).load().language
        super.attachBaseContext(LocaleManager.wrapContext(newBase, lang))
    }
}
