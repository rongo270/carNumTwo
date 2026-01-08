package com.rongo.carnumtwo.feature.settings

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.config.SpeedOptions
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity

class SettingsActivity : BaseLocalizedActivity() {

    private lateinit var storage: SettingsStorage

    // Grid Variables
    private var currentX = GameDefaults.DEFAULT_GRID_X
    private var currentY = GameDefaults.DEFAULT_GRID_Y

    // Speed Variables (default to Medium)
    private var selectedSpeedLevel = SpeedOptions.SpeedLevel.MEDIUM

    // UI References
    private lateinit var tvValX: TextView
    private lateinit var tvValY: TextView
    private lateinit var btnSpeedSlow: Button
    private lateinit var btnSpeedMedium: Button
    private lateinit var btnSpeedFast: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        storage = SettingsStorage(this)

        // Bind Views
        tvValX = findViewById(R.id.tv_val_x)
        tvValY = findViewById(R.id.tv_val_y)
        btnSpeedSlow = findViewById(R.id.btn_speed_slow)
        btnSpeedMedium = findViewById(R.id.btn_speed_medium)
        btnSpeedFast = findViewById(R.id.btn_speed_fast)

        // Load existing values
        val savedSettings = storage.load()
        currentX = savedSettings.gridX
        currentY = savedSettings.gridY
        selectedSpeedLevel = SpeedOptions.getLevel(savedSettings.tickMs)

        // Setup Grid Controls (Horizontal - / +)
        setupGridXControl()
        setupGridYControl()

        // Setup Speed Controls (3 Buttons)
        setupSpeedButtons()

        // Apply Button Logic
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            saveAndExit()
        }
    }

    private fun setupGridXControl() {
        updateXDisplay()
        findViewById<Button>(R.id.btn_dec_x).setOnClickListener {
            if (currentX > GameDefaults.MIN_GRID_X) {
                currentX--
                updateXDisplay()
            }
        }
        findViewById<Button>(R.id.btn_inc_x).setOnClickListener {
            if (currentX < GameDefaults.MAX_GRID_X) {
                currentX++
                updateXDisplay()
            }
        }
    }

    private fun updateXDisplay() {
        tvValX.text = currentX.toString()
    }

    private fun setupGridYControl() {
        updateYDisplay()
        findViewById<Button>(R.id.btn_dec_y).setOnClickListener {
            if (currentY > GameDefaults.MIN_GRID_Y) {
                currentY--
                updateYDisplay()
            }
        }
        findViewById<Button>(R.id.btn_inc_y).setOnClickListener {
            if (currentY < GameDefaults.MAX_GRID_Y) {
                currentY++
                updateYDisplay()
            }
        }
    }

    private fun updateYDisplay() {
        tvValY.text = currentY.toString()
    }

    private fun setupSpeedButtons() {
        // Initial Visual Update
        updateSpeedVisuals()

        btnSpeedSlow.setOnClickListener {
            selectedSpeedLevel = SpeedOptions.SpeedLevel.SLOW
            updateSpeedVisuals()
        }

        btnSpeedMedium.setOnClickListener {
            selectedSpeedLevel = SpeedOptions.SpeedLevel.MEDIUM
            updateSpeedVisuals()
        }

        btnSpeedFast.setOnClickListener {
            selectedSpeedLevel = SpeedOptions.SpeedLevel.FAST
            updateSpeedVisuals()
        }
    }

    // Highlights the selected button and dims the others
    private fun updateSpeedVisuals() {
        // Reset all to unselected style (Secondary BG, secondary text)
        setButtonStyle(btnSpeedSlow, false)
        setButtonStyle(btnSpeedMedium, false)
        setButtonStyle(btnSpeedFast, false)

        // Highlight the selected one (Primary BG, white text)
        when (selectedSpeedLevel) {
            SpeedOptions.SpeedLevel.SLOW -> setButtonStyle(btnSpeedSlow, true)
            SpeedOptions.SpeedLevel.MEDIUM -> setButtonStyle(btnSpeedMedium, true)
            SpeedOptions.SpeedLevel.FAST -> setButtonStyle(btnSpeedFast, true)
            else -> setButtonStyle(btnSpeedMedium, true) // Default fallback
        }
    }

    private fun setButtonStyle(btn: Button, isSelected: Boolean) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.btn_primary_bg)
            btn.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            btn.setBackgroundResource(R.drawable.btn_secondary_bg)
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun saveAndExit() {
        // 1. Save Grid
        storage.saveGrid(currentX, currentY)

        // 2. Determine MS values based on selected level
        val (tick, spawn) = when (selectedSpeedLevel) {
            SpeedOptions.SpeedLevel.SLOW -> Pair(SpeedOptions.SLOW_TICK_MS, SpeedOptions.SLOW_SPAWN_MS)
            SpeedOptions.SpeedLevel.MEDIUM -> Pair(SpeedOptions.MEDIUM_TICK_MS, SpeedOptions.MEDIUM_SPAWN_MS)
            SpeedOptions.SpeedLevel.FAST -> Pair(SpeedOptions.FAST_TICK_MS, SpeedOptions.FAST_SPAWN_MS)
            else -> Pair(SpeedOptions.MEDIUM_TICK_MS, SpeedOptions.MEDIUM_SPAWN_MS)
        }
        storage.saveTiming(tick, spawn)

        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}