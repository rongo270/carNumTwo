// English comments only inside code
package com.rongo.carnumtwo.feature.settings

import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity

class SettingsActivity : BaseLocalizedActivity() {

    private lateinit var storage: SettingsStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        storage = SettingsStorage(this)

        val npX = findViewById<NumberPicker>(R.id.np_x)
        val npY = findViewById<NumberPicker>(R.id.np_y)
        val btnApply = findViewById<Button>(R.id.btn_apply)

        val current = storage.load()

        npX.minValue = GameDefaults.MIN_GRID_X
        npX.maxValue = GameDefaults.MAX_GRID_X
        npX.value = current.gridX
        npX.wrapSelectorWheel = true

        npY.minValue = GameDefaults.MIN_GRID_Y
        npY.maxValue = GameDefaults.MAX_GRID_Y
        npY.value = current.gridY
        npY.wrapSelectorWheel = true

        btnApply.setOnClickListener {
            storage.saveGrid(npX.value, npY.value)
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
