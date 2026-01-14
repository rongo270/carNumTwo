package com.rongo.carnumtwo.feature.score

import android.os.Bundle
import android.widget.Button
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity

class ScoreActivity : BaseLocalizedActivity() {

    private lateinit var mapFragment: ScoreMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        // Initialize Fragments
        val listFragment = ScoreListFragment()
        mapFragment = ScoreMapFragment()

        // Place fragments into their containers
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_list_container, listFragment)
            .replace(R.id.fragment_map_container, mapFragment)
            .commit()

        // Connect List click to Map focus
        listFragment.onScoreClicked = { lat, lon ->
            mapFragment.focusOnLocation(lat, lon)
        }

        // Back Button Logic
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
}