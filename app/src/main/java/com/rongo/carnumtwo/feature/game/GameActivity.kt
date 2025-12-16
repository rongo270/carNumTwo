// English comments only inside code
package com.rongo.carnumtwo.feature.game

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity
import com.rongo.carnumtwo.feature.game.engine.GameController
import com.rongo.carnumtwo.feature.game.engine.GameLoop
import com.rongo.carnumtwo.feature.game.engine.GameUiCallbacks
import com.rongo.carnumtwo.feature.game.model.GameState
import com.rongo.carnumtwo.feature.game.render.GameRenderer
import com.rongo.carnumtwo.feature.menu.StartMenuActivity
import kotlin.math.max
import kotlin.math.min

class GameActivity : BaseLocalizedActivity(), GameUiCallbacks {

    private lateinit var root: View
    private lateinit var grid: GridLayout
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btnPause: ImageButton

    private lateinit var tvScore: TextView
    private lateinit var heart1: ImageView
    private lateinit var heart2: ImageView
    private lateinit var heart3: ImageView

    private lateinit var controller: GameController
    private lateinit var renderer: GameRenderer
    private lateinit var loop: GameLoop

    private lateinit var state: GameState
    private lateinit var cells: Array<Array<ImageView>>

    private var gameOverShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        root = findViewById(R.id.game_root)
        grid = findViewById(R.id.game_grid)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        btnPause = findViewById(R.id.btn_pause)

        tvScore = findViewById(R.id.tv_score)
        heart1 = findViewById(R.id.heart_1)
        heart2 = findViewById(R.id.heart_2)
        heart3 = findViewById(R.id.heart_3)

        applyBottomInsetsToRoot()

        val settings = SettingsStorage(this).load()
        val cols = settings.gridX
        val rows = settings.gridY

        state = GameState(
            cols = cols,
            rows = rows,
            playerCol = cols / 2,
            paused = false,
            lives = 3,
            score = 0,
            invulnerableUntilMs = 0L,
            chickens = mutableListOf()
        )

        setupGrid(cols, rows)

        renderer = GameRenderer(cells, rows, cols)
        controller = GameController(
            state = state,
            renderer = renderer,
            ui = this,
            invulnerableMs = GameDefaults.INVULNERABLE_MS
        )
        controller.init()

        loop = GameLoop(
            tickMs = settings.tickMs,
            spawnMs = settings.spawnMs,
            controller = controller
        )
        loop.start()

        btnLeft.setOnClickListener { controller.moveLeft() }
        btnRight.setOnClickListener { controller.moveRight() }

        btnPause.setOnClickListener { togglePauseByUser() }
        updatePauseIcon()
    }

    override fun onPause() {
        super.onPause()
        // Auto-pause when app goes to background
        if (!controller.isPaused()) controller.setPaused(true)
        updatePauseIcon()

        // Stop scheduling to save resources while in background
        loop.stop()
    }

    override fun onResume() {
        super.onResume()
        // Resume scheduling, but game remains paused until user presses play
        if (!gameOverShown) loop.start()
        updatePauseIcon()
        renderer.render(state)
    }

    override fun onDestroy() {
        super.onDestroy()
        loop.stop()
    }

    private fun togglePauseByUser() {
        controller.setPaused(!controller.isPaused())
        updatePauseIcon()
        renderer.render(state)
    }

    private fun updatePauseIcon() {
        btnPause.setImageResource(if (controller.isPaused()) R.drawable.ic_play else R.drawable.ic_pause)
    }

    private fun applyBottomInsetsToRoot() {
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, baseBottom + bars.bottom)
            insets
        }
    }

    private fun setupGrid(cols: Int, rows: Int) {
        grid.removeAllViews()
        grid.columnCount = cols
        grid.rowCount = rows

        cells = Array(rows) { r ->
            Array(cols) {
                val cell = AppCompatImageView(this)
                cell.scaleType = ImageView.ScaleType.FIT_CENTER
                cell.setBackgroundResource(R.drawable.bg_cell)
                grid.addView(cell)
                cell
            }
        }

        grid.post {
            val margin = dpToPx(6)

            val availableW = max(0, grid.width - (cols * margin * 2))
            val availableH = max(0, grid.height - (rows * margin * 2))

            val cellSizeByW = if (cols > 0) availableW / cols else 0
            val cellSizeByH = if (rows > 0) availableH / rows else 0
            val cellSize = max(1, min(cellSizeByW, cellSizeByH))

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val lp = GridLayout.LayoutParams().apply {
                        width = cellSize
                        height = cellSize
                        rowSpec = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                        setMargins(margin, margin, margin, margin)
                    }
                    cells[r][c].layoutParams = lp
                }
            }

            val contentW = cols * cellSize + cols * margin * 2
            val contentH = rows * cellSize + rows * margin * 2
            val padX = max(0, (grid.width - contentW) / 2)
            val padY = max(0, (grid.height - contentH) / 2)

            grid.setPadding(padX, padY, padX, padY)
            grid.clipToPadding = false

            renderer.render(state)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // GameUiCallbacks

    override fun updateHearts(lives: Int) {
        val full = R.drawable.ic_heart_full
        val empty = R.drawable.ic_heart_empty

        heart1.setImageResource(if (lives >= 1) full else empty)
        heart2.setImageResource(if (lives >= 2) full else empty)
        heart3.setImageResource(if (lives >= 3) full else empty)
    }

    override fun updateScore(score: Int) {
        tvScore.text = getString(R.string.score_label, score)
    }

    override fun showHitFeedback() {
        Toast.makeText(this, getString(R.string.hit_toast), Toast.LENGTH_SHORT).show()
        vibrateHit()
    }

    private fun vibrateHit() {
        val durationMs = 120L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }

    override fun showGameOverDialog(finalScore: Int) {
        gameOverShown = true
        loop.stop()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.game_over_title))
            .setMessage(getString(R.string.game_over_message, finalScore))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.restart)) { _, _ ->
                gameOverShown = false
                controller.resetGame()
                loop.start()
                updatePauseIcon()
            }
            .setNeutralButton(getString(R.string.home)) { _, _ ->
                startActivity(Intent(this, StartMenuActivity::class.java))
                finish()
            }
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                finishAffinity()
            }
            .show()
    }
}
