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
import com.rongo.carnumtwo.feature.score.ScoreActivity
import com.rongo.carnumtwo.feature.settings.SettingsActivity

class StartMenuActivity : BaseLocalizedActivity() {

    private lateinit var storage: SettingsStorage
    private lateinit var tvSummaryGrid: TextView
    private lateinit var tvSummarySpeed: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        storage = SettingsStorage(this)

        tvSummaryGrid = findViewById(R.id.tv_summary_grid)
        tvSummarySpeed = findViewById(R.id.tv_summary_speed)

        // Start Game
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        // Open Settings
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Open High Scores (UPDATED)
        findViewById<Button>(R.id.btn_score).setOnClickListener {
            startActivity(Intent(this, ScoreActivity::class.java))
        }

        // Exit App
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finishAffinity()
        }

        // Toggle Language
        findViewById<Button>(R.id.btn_language).setOnClickListener {
            toggleLanguage()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSummary()
    }

    private fun updateSummary() {
        val s = storage.load()
        tvSummaryGrid.text = getString(R.string.summary_grid, s.gridX, s.gridY)
        tvSummarySpeed.text = getString(
            R.string.summary_speed,
            s.tickMs / 1000.0,
            s.spawnMs / 1000.0
        )
    }

    private fun toggleLanguage() {
        val current = storage.load().language
        val next = if (current == "en") "he" else "en"
        storage.saveLanguage(next)
        recreate()
    }
}