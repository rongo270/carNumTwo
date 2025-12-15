// English comments only inside code
package com.rongo.carnumtwo.feature.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.feature.game.GameActivity
import com.rongo.carnumtwo.feature.settings.SettingsActivity

class StartMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_score).setOnClickListener {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finishAffinity()
        }
    }
}
