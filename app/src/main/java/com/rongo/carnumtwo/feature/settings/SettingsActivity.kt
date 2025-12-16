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
        // Inflate the settings screen layout
        setContentView(R.layout.activity_settings)

        // Storage helper for saving/loading settings
        storage = SettingsStorage(this)

        // Bind views
        val npX = findViewById<NumberPicker>(R.id.np_x)
        val npY = findViewById<NumberPicker>(R.id.np_y)
        val npTick = findViewById<NumberPicker>(R.id.np_tick)
        val npSpawn = findViewById<NumberPicker>(R.id.np_spawn)
        val btnApply = findViewById<Button>(R.id.btn_apply)

        // Load current saved settings
        val current = storage.load()

        // Grid X picker setup
        npX.minValue = GameDefaults.MIN_GRID_X
        npX.maxValue = GameDefaults.MAX_GRID_X
        npX.value = current.gridX
        npX.wrapSelectorWheel = true

        // Grid Y picker setup
        npY.minValue = GameDefaults.MIN_GRID_Y
        npY.maxValue = GameDefaults.MAX_GRID_Y
        npY.value = current.gridY
        npY.wrapSelectorWheel = true

        // Tick picker setup (predefined ms options)
        setupMsPicker(
            picker = npTick,
            optionsMs = SpeedOptions.TICK_OPTIONS_MS,
            currentMs = current.tickMs
        )

        // Spawn picker setup (predefined ms options)
        setupMsPicker(
            picker = npSpawn,
            optionsMs = SpeedOptions.SPAWN_OPTIONS_MS,
            currentMs = current.spawnMs
        )

        // Save settings and close the screen
        btnApply.setOnClickListener {
            // Save grid sizes
            storage.saveGrid(npX.value, npY.value)

            // Save timing values based on selected indices
            val tickMs = SpeedOptions.TICK_OPTIONS_MS[npTick.value]
            val spawnMs = SpeedOptions.SPAWN_OPTIONS_MS[npSpawn.value]
            storage.saveTiming(tickMs, spawnMs)

            // Show confirmation and exit
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Configure a NumberPicker to show time options (ms) as seconds labels
    private fun setupMsPicker(picker: NumberPicker, optionsMs: LongArray, currentMs: Long) {
        picker.minValue = 0
        picker.maxValue = optionsMs.size - 1
        picker.wrapSelectorWheel = true

        // Build displayed labels (e.g., "0.5s", "1.0s")
        val labels = Array(optionsMs.size) { i ->
            formatSeconds(optionsMs[i])
        }
        picker.displayedValues = labels

        // Select the closest option to what is currently saved
        picker.value = findNearestIndex(optionsMs, currentMs)
    }

    // Find the index of the option that is closest to the target value
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

    // Convert ms to a simple seconds string with 1 decimal (e.g., 1500 -> "1.5s")
    private fun formatSeconds(ms: Long): String {
        val seconds = ms / 1000.0
        return String.format("%.1fs", seconds)
    }
}
