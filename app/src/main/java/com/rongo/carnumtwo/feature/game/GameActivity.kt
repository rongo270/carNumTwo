package com.rongo.carnumtwo.feature.game

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
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
import com.rongo.carnumtwo.core.audio.SoundManager
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity
import com.rongo.carnumtwo.feature.game.engine.BulletManager
import com.rongo.carnumtwo.feature.game.engine.GameController
import com.rongo.carnumtwo.feature.game.engine.GameLoop
import com.rongo.carnumtwo.feature.game.engine.GameUiCallbacks
import com.rongo.carnumtwo.feature.game.model.GameState
import com.rongo.carnumtwo.feature.game.render.GameRenderer
import com.rongo.carnumtwo.feature.menu.StartMenuActivity
import kotlin.math.max
import kotlin.math.min

class GameActivity : BaseLocalizedActivity(), GameUiCallbacks, SensorEventListener {

    // UI Views
    private lateinit var root: View
    private lateinit var grid: GridLayout
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btnFire: ImageButton
    private lateinit var btnPause: ImageButton

    private lateinit var tvScore: TextView
    private lateinit var tvCoins: TextView
    private lateinit var heart1: ImageView
    private lateinit var heart2: ImageView
    private lateinit var heart3: ImageView

    // Game Engine Components
    private lateinit var controller: GameController
    private lateinit var renderer: GameRenderer
    private lateinit var loop: GameLoop

    private lateinit var state: GameState
    private lateinit var cells: Array<Array<ImageView>>

    // Audio Manager
    private lateinit var soundManager: SoundManager

    // Game State Flag
    private var gameOverShown = false

    // Sensors Variables
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isTiltEnabled = false
    private var lastTiltMoveTime = 0L
    private val TILT_MOVE_COOLDOWN = 150L
    private val TILT_THRESHOLD = 1.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // 1. Initialize Sound Manager
        soundManager = SoundManager(this)

        // Bind views
        root = findViewById(R.id.game_root)
        grid = findViewById(R.id.game_grid)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        btnFire = findViewById(R.id.btn_fire)
        btnPause = findViewById(R.id.btn_pause)

        tvScore = findViewById(R.id.tv_score)
        tvCoins = findViewById(R.id.tv_coins)
        heart1 = findViewById(R.id.heart_1)
        heart2 = findViewById(R.id.heart_2)
        heart3 = findViewById(R.id.heart_3)

        applyBottomInsetsToRoot()

        // 2. Load settings
        val settings = SettingsStorage(this).load()
        val cols = settings.gridX
        val rows = settings.gridY
        val initialSpeed = settings.tickMs

        // 3. Apply Audio Settings (Volume 0-100)
        soundManager.setVolumes(settings.musicVolume, settings.sfxVolume)

        // --- Control Setup ---
        // Configure Buttons Visibility
        if (settings.enableButtons) {
            btnLeft.visibility = View.VISIBLE
            btnRight.visibility = View.VISIBLE
        } else {
            btnLeft.visibility = View.GONE
            btnRight.visibility = View.GONE
        }

        // Configure Tilt Sensor
        isTiltEnabled = settings.enableTilt
        if (isTiltEnabled) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        // Create initial game state
        state = GameState(
            cols = cols,
            rows = rows,
            playerCol = cols / 2,
            paused = false,
            lives = 3,
            score = 0,
            coinsCollected = 0,
            invulnerableUntilMs = 0L,
            chickens = mutableListOf(),
            bullets = mutableListOf(),
            coins = mutableListOf(),
            lastShotAtMs = 0L
        )

        setupGrid(cols, rows)
        renderer = GameRenderer(cells, rows, cols)

        val bulletManager = BulletManager(shotCooldownMs = 200L)

        // Initialize Controller with initial speed from settings
        controller = GameController(
            state = state,
            renderer = renderer,
            ui = this,
            invulnerableMs = GameDefaults.INVULNERABLE_MS,
            bulletManager = bulletManager,
            initialTickMs = initialSpeed
        )
        controller.init()

        // Start the master game loop
        loop = GameLoop(controller)
        loop.start()

        // Set Button Listeners
        btnLeft.setOnClickListener { controller.moveLeft() }
        btnRight.setOnClickListener { controller.moveRight() }
        btnFire.setOnClickListener { controller.shoot() }

        btnPause.setOnClickListener { togglePauseByUser() }
        updatePauseIcon()
    }

    override fun onResume() {
        super.onResume()
        if (!gameOverShown) {
            loop.start()
            // Resume music
            soundManager.startMusic()
        }
        updatePauseIcon()
        renderer.render(state)

        if (isTiltEnabled && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!controller.isPaused()) controller.setPaused(true)
        updatePauseIcon()
        loop.stop()

        // Pause music when app goes background
        soundManager.pauseMusic()

        if (isTiltEnabled) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loop.stop()
        soundManager.release() // Release audio resources
    }

    // --- Audio Implementation ---
    override fun playSoundMove() {
        soundManager.playMove()
    }

    override fun playSoundExplosion() {
        soundManager.playExplode()
    }

    override fun playSoundCoin() {
        soundManager.playCoin()
    }

    // --- Sensor Logic (Tilt Control) ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || controller.isPaused() || gameOverShown) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val now = SystemClock.uptimeMillis()

            // Throttle sensor events to avoid too fast movement
            if (now - lastTiltMoveTime > TILT_MOVE_COOLDOWN) {
                if (x < -TILT_THRESHOLD) {
                    controller.moveRight()
                    lastTiltMoveTime = now
                }
                else if (x > TILT_THRESHOLD) {
                    controller.moveLeft()
                    lastTiltMoveTime = now
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    // --- Game Logic Helpers ---
    private fun togglePauseByUser() {
        val wasPaused = controller.isPaused()
        controller.setPaused(!wasPaused)
        updatePauseIcon()
        renderer.render(state)

        // Toggle music based on pause state
        if (!wasPaused) {
            // Game is now PAUSED
            soundManager.pauseMusic()
        } else {
            // Game is now RESUMED
            soundManager.startMusic()
        }
    }

    private fun updatePauseIcon() {
        btnPause.setImageResource(if (controller.isPaused()) R.drawable.ic_play else R.drawable.ic_pause)
    }

    // Handle Edge-to-Edge display
    private fun applyBottomInsetsToRoot() {
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, baseBottom + bars.bottom)
            insets
        }
    }

    // Setup GridView dynamically
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

        // Calculate cell size after layout measure
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

            // Center grid in container
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

    // --- UI Update Callbacks ---
    override fun updateHearts(lives: Int) {
        setHeart(heart1, lives >= 1)
        setHeart(heart2, lives >= 2)
        setHeart(heart3, lives >= 3)
    }

    private fun setHeart(view: ImageView, isAlive: Boolean) {
        view.setImageResource(R.drawable.ic_heart_full)
        view.alpha = if (isAlive) 1f else 0.25f
    }

    override fun updateScore(score: Int) {
        tvScore.text = getString(R.string.score_label, score)
    }

    override fun updateCoins(coins: Int) {
        tvCoins.text = coins.toString()
    }

    override fun showHitFeedback() {
        val toast = Toast.makeText(this, getString(R.string.hit_toast), Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, computeTopToastYOffset())
        toast.show()
        vibrateHit()
    }

    private fun computeTopToastYOffset(): Int {
        val insets = ViewCompat.getRootWindowInsets(root)
        val topInset = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
        return topInset + dpToPx(72)
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
        soundManager.pauseMusic()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.game_over_title))
            .setMessage(getString(R.string.game_over_message, finalScore))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.restart)) { _, _ ->
                gameOverShown = false
                controller.resetGame()
                loop.start()
                soundManager.startMusic()
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