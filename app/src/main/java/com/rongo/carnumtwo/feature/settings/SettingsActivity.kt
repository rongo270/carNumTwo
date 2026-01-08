package com.rongo.carnumtwo.feature.settings

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
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

    // Speed Variables
    private var selectedSpeedLevel = SpeedOptions.SpeedLevel.MEDIUM

    // Control Variables
    private var isButtonsEnabled = true
    private var isTiltEnabled = false

    // Audio Variables
    private var musicVolume = 100
    private var sfxVolume = 100

    // UI References
    private lateinit var tvValX: TextView
    private lateinit var tvValY: TextView

    private lateinit var btnSpeedSlow: Button
    private lateinit var btnSpeedMedium: Button
    private lateinit var btnSpeedFast: Button

    private lateinit var btnCtrlButtons: Button
    private lateinit var btnCtrlTilt: Button

    // New SeekBars
    private lateinit var seekMusic: SeekBar
    private lateinit var seekSfx: SeekBar

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

        btnCtrlButtons = findViewById(R.id.btn_ctrl_buttons)
        btnCtrlTilt = findViewById(R.id.btn_ctrl_tilt)

        seekMusic = findViewById(R.id.seek_music)
        seekSfx = findViewById(R.id.seek_sfx)

        // Load existing values
        val savedSettings = storage.load()
        currentX = savedSettings.gridX
        currentY = savedSettings.gridY
        selectedSpeedLevel = SpeedOptions.getLevel(savedSettings.tickMs)
        isButtonsEnabled = savedSettings.enableButtons
        isTiltEnabled = savedSettings.enableTilt

        // Load Audio
        musicVolume = savedSettings.musicVolume
        sfxVolume = savedSettings.sfxVolume

        // Initialize SeekBars
        seekMusic.progress = musicVolume
        seekSfx.progress = sfxVolume

        // Setup UI Sections
        setupGridXControl()
        setupGridYControl()
        setupSpeedButtons()
        setupControlButtons()
        setupAudioSeekBars()

        // Apply Button Logic
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            saveAndExit()
        }
    }

    private fun setupAudioSeekBars() {
        seekMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicVolume = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekSfx.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sfxVolume = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // --- Controls Section ---
    private fun setupControlButtons() {
        updateControlVisuals()

        btnCtrlButtons.setOnClickListener {
            isButtonsEnabled = !isButtonsEnabled
            if (!isButtonsEnabled && !isTiltEnabled) isButtonsEnabled = true
            updateControlVisuals()
        }

        btnCtrlTilt.setOnClickListener {
            isTiltEnabled = !isTiltEnabled
            if (!isButtonsEnabled && !isTiltEnabled) isTiltEnabled = true
            updateControlVisuals()
        }
    }

    private fun updateControlVisuals() {
        setButtonStyle(btnCtrlButtons, isButtonsEnabled)
        setButtonStyle(btnCtrlTilt, isTiltEnabled)
    }

    // --- Grid Section ---
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

    // --- Speed Section ---
    private fun setupSpeedButtons() {
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

    private fun updateSpeedVisuals() {
        setButtonStyle(btnSpeedSlow, selectedSpeedLevel == SpeedOptions.SpeedLevel.SLOW)
        setButtonStyle(btnSpeedMedium, selectedSpeedLevel == SpeedOptions.SpeedLevel.MEDIUM)
        setButtonStyle(btnSpeedFast, selectedSpeedLevel == SpeedOptions.SpeedLevel.FAST)
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
        storage.saveGrid(currentX, currentY)

        val (tick, spawn) = when (selectedSpeedLevel) {
            SpeedOptions.SpeedLevel.SLOW -> Pair(SpeedOptions.SLOW_TICK_MS, SpeedOptions.SLOW_SPAWN_MS)
            SpeedOptions.SpeedLevel.MEDIUM -> Pair(SpeedOptions.MEDIUM_TICK_MS, SpeedOptions.MEDIUM_SPAWN_MS)
            SpeedOptions.SpeedLevel.FAST -> Pair(SpeedOptions.FAST_TICK_MS, SpeedOptions.FAST_SPAWN_MS)
            else -> Pair(SpeedOptions.MEDIUM_TICK_MS, SpeedOptions.MEDIUM_SPAWN_MS)
        }
        storage.saveTiming(tick, spawn)
        storage.saveControls(isButtonsEnabled, isTiltEnabled)
        storage.saveAudio(musicVolume, sfxVolume) // Save integer volumes

        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}