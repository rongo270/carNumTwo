package com.rongo.carnumtwo.feature.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity
import com.rongo.carnumtwo.feature.game.GameActivity
import com.rongo.carnumtwo.feature.settings.SettingsActivity

class StartMenuActivity : BaseLocalizedActivity() {

    private lateinit var storage: SettingsStorage
    private lateinit var tvSummaryGrid: TextView
    private lateinit var tvSummarySpeed: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the start menu layout
        setContentView(R.layout.activity_start_menu)

        // Storage helper for loading settings
        storage = SettingsStorage(this)

        // Bind summary views
        tvSummaryGrid = findViewById(R.id.tv_summary_grid)
        tvSummarySpeed = findViewById(R.id.tv_summary_speed)

        // Start the game
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        // Open settings screen
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Placeholder score button
        findViewById<Button>(R.id.btn_score).setOnClickListener {
            Toast.makeText(this, getString(R.string.score_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Exit the app
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finishAffinity()
        }

        // Toggle language (en/he) and recreate the activity
        findViewById<Button>(R.id.btn_language).setOnClickListener {
            toggleLanguage()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh summary when returning from other screens
        updateSummary()
    }

    // Update the summary text based on saved settings
    private fun updateSummary() {
        val s = storage.load()
        tvSummaryGrid.text = getString(R.string.summary_grid, s.gridX, s.gridY)
        tvSummarySpeed.text = getString(
            R.string.summary_speed,
            s.tickMs / 1000.0,
            s.spawnMs / 1000.0
        )
    }

    // Switch between English and Hebrew and reload UI
    private fun toggleLanguage() {
        val current = storage.load().language
        val next = if (current == "en") "he" else "en"
        storage.saveLanguage(next)
        recreate()
    }
}
