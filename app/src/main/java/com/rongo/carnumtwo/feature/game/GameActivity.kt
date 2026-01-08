package com.rongo.carnumtwo.feature.game

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
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

    private lateinit var controller: GameController
    private lateinit var renderer: GameRenderer
    private lateinit var loop: GameLoop

    private lateinit var state: GameState
    private lateinit var cells: Array<Array<ImageView>>

    private lateinit var soundManager: SoundManager

    private var gameOverShown = false

    // Animator for shoot button cooldown
    private var cooldownAnimator: ValueAnimator? = null

    // Sensors
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isTiltEnabled = false
    private var lastTiltMoveTime = 0L
    private val TILT_MOVE_COOLDOWN = 150L
    private val TILT_THRESHOLD = 1.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        soundManager = SoundManager(this)

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

        // *** Set the Cooldown Background ***
        btnFire.setBackgroundResource(R.drawable.bg_fire_button_cooldown)
        // Start full (Level 10000 = 100%)
        setButtonCooldownLevel(10000)

        applyBottomInsetsToRoot()

        val settings = SettingsStorage(this).load()
        val cols = settings.gridX
        val rows = settings.gridY
        val initialSpeed = settings.tickMs

        soundManager.setVolumes(settings.musicVolume, settings.sfxVolume)

        if (settings.enableButtons) {
            btnLeft.visibility = View.VISIBLE
            btnRight.visibility = View.VISIBLE
        } else {
            btnLeft.visibility = View.GONE
            btnRight.visibility = View.GONE
        }

        isTiltEnabled = settings.enableTilt
        if (isTiltEnabled) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

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

        // Pass 3000ms cooldown here
        val bulletManager = BulletManager(shotCooldownMs = GameDefaults.SHOOT_COOLDOWN_MS)

        controller = GameController(
            state = state,
            renderer = renderer,
            ui = this,
            invulnerableMs = GameDefaults.INVULNERABLE_MS,
            bulletManager = bulletManager,
            initialTickMs = initialSpeed
        )
        controller.init()

        loop = GameLoop(controller)
        loop.start()

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

        soundManager.pauseMusic()
        cooldownAnimator?.cancel() // Stop animation on pause

        if (isTiltEnabled) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loop.stop()
        soundManager.release()
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

    // --- Cooldown UI Implementation ---

    override fun onShootSuccess(cooldownMs: Long) {
        // Start animation from 0 (empty) to 10000 (full) over 3 seconds
        animateCooldown(cooldownMs)
    }

    override fun onShootFailed() {
        // Locked sound
        soundManager.playLocked()

        // Shake animation
        btnFire.animate()
            .translationX(10f)
            .setDuration(50)
            .withEndAction {
                btnFire.animate().translationX(-10f).setDuration(50).withEndAction {
                    btnFire.animate().translationX(0f).setDuration(50).start()
                }.start()
            }.start()
    }

    private fun animateCooldown(durationMs: Long) {
        if (durationMs <= 0) {
            setButtonCooldownLevel(10000)
            return
        }

        cooldownAnimator?.cancel()

        cooldownAnimator = ValueAnimator.ofInt(0, 10000).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val level = animator.animatedValue as Int
                setButtonCooldownLevel(level)
            }
            start()
        }
    }

    private fun setButtonCooldownLevel(level: Int) {
        // Access the LayerDrawable and then the ClipDrawable
        val layerDrawable = btnFire.background as? LayerDrawable
        val clipDrawable = layerDrawable?.findDrawableByLayerId(android.R.id.progress) as? ClipDrawable
        clipDrawable?.level = level
    }

    // --- Sensor Logic ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || controller.isPaused() || gameOverShown) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val now = SystemClock.uptimeMillis()
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

        if (!wasPaused) {
            soundManager.pauseMusic()
            cooldownAnimator?.pause()
        } else {
            soundManager.startMusic()
            cooldownAnimator?.resume()
        }
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
        cooldownAnimator?.cancel()

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