// English comments only inside code
package com.rongo.carnumtwo.feature.settings

import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.config.SpeedOptions
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity
import kotlin.math.abs

class SettingsActivity : BaseLocalizedActivity() {

    private lateinit var storage: SettingsStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        storage = SettingsStorage(this)

        val npX = findViewById<NumberPicker>(R.id.np_x)
        val npY = findViewById<NumberPicker>(R.id.np_y)
        val npTick = findViewById<NumberPicker>(R.id.np_tick)
        val npSpawn = findViewById<NumberPicker>(R.id.np_spawn)
        val btnApply = findViewById<Button>(R.id.btn_apply)

        val current = storage.load()

        // Grid pickers
        npX.minValue = GameDefaults.MIN_GRID_X
        npX.maxValue = GameDefaults.MAX_GRID_X
        npX.value = current.gridX
        npX.wrapSelectorWheel = true

        npY.minValue = GameDefaults.MIN_GRID_Y
        npY.maxValue = GameDefaults.MAX_GRID_Y
        npY.value = current.gridY
        npY.wrapSelectorWheel = true

        // Tick picker (options list)
        setupMsPicker(
            picker = npTick,
            optionsMs = SpeedOptions.TICK_OPTIONS_MS,
            currentMs = current.tickMs
        )

        // Spawn picker (options list)
        setupMsPicker(
            picker = npSpawn,
            optionsMs = SpeedOptions.SPAWN_OPTIONS_MS,
            currentMs = current.spawnMs
        )

        btnApply.setOnClickListener {
            storage.saveGrid(npX.value, npY.value)

            val tickMs = SpeedOptions.TICK_OPTIONS_MS[npTick.value]
            val spawnMs = SpeedOptions.SPAWN_OPTIONS_MS[npSpawn.value]
            storage.saveTiming(tickMs, spawnMs)

            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupMsPicker(picker: NumberPicker, optionsMs: LongArray, currentMs: Long) {
        picker.minValue = 0
        picker.maxValue = optionsMs.size - 1
        picker.wrapSelectorWheel = true

        val labels = Array(optionsMs.size) { i ->
            formatSeconds(optionsMs[i])
        }
        picker.displayedValues = labels

        picker.value = findNearestIndex(optionsMs, currentMs)
    }

    private fun findNearestIndex(options: LongArray, target: Long): Int {
        var bestIdx = 0
        var bestDiff = Long.MAX_VALUE
        for (i in options.indices) {
            val diff = abs(options[i] - target)
            if (diff < bestDiff) {
                bestDiff = diff
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun formatSeconds(ms: Long): String {
        val seconds = ms / 1000.0
        return String.format("%.1fs", seconds)
    }
}
