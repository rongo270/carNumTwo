package com.rongo.carnumtwo.feature.game

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
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
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Kept just in case, but mostly unused now
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

    // --- UI Views ---
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

    // --- Game Engine ---
    private lateinit var controller: GameController
    private lateinit var renderer: GameRenderer
    private lateinit var loop: GameLoop

    private lateinit var state: GameState
    private lateinit var cells: Array<Array<ImageView>>

    // --- Managers ---
    private lateinit var soundManager: SoundManager

    // --- State Flags ---
    private var gameOverShown = false

    // --- Animation ---
    // Animator for shoot button cooldown
    private var cooldownAnimator: ValueAnimator? = null

    // --- Sensors ---
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

        // 2. Bind views
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

        // 3. Setup Fire Button Visuals (Cooldown Background)
        btnFire.setBackgroundResource(R.drawable.bg_fire_button_cooldown)
        // Start full (Level 10000 = 100%)
        setButtonCooldownLevel(10000)

        // 4. Handle Edge-to-Edge padding
        applyBottomInsetsToRoot()

        // 5. Load settings
        val settings = SettingsStorage(this).load()
        val cols = settings.gridX
        val rows = settings.gridY
        val initialSpeed = settings.tickMs

        // Apply initial volume settings
        soundManager.setVolumes(settings.musicVolume, settings.sfxVolume)

        // --- Control Visibility Setup ---
        if (settings.enableButtons) {
            btnLeft.visibility = View.VISIBLE
            btnRight.visibility = View.VISIBLE
        } else {
            btnLeft.visibility = View.GONE
            btnRight.visibility = View.GONE
        }

        // --- Sensor Setup ---
        isTiltEnabled = settings.enableTilt
        if (isTiltEnabled) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        // 6. Initialize Game State
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

        // Initialize Managers
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

        // Start Loop
        loop = GameLoop(controller)
        loop.start()

        // 7. Click Listeners
        btnLeft.setOnClickListener { controller.moveLeft() }
        btnRight.setOnClickListener { controller.moveRight() }
        btnFire.setOnClickListener { controller.shoot() }

        btnPause.setOnClickListener { togglePauseByUser() }
        updatePauseIcon()
    }

    // --- Lifecycle Methods ---

    override fun onResume() {
        super.onResume()
        if (!gameOverShown) {
            loop.start()
            // Resume music if enabled
            soundManager.startMusic()
        }
        updatePauseIcon()
        renderer.render(state)

        // Re-register sensor
        if (isTiltEnabled && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Auto-pause game logic when app is backgrounded
        if (!controller.isPaused()) controller.setPaused(true)
        updatePauseIcon()
        loop.stop()

        soundManager.pauseMusic()
        cooldownAnimator?.cancel() // Stop UI animation

        if (isTiltEnabled) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loop.stop()
        soundManager.release() // Cleanup audio
    }

    // --- Audio Implementation (GameUiCallbacks) ---
    override fun playSoundMove() {
        soundManager.playMove()
    }

    override fun playSoundExplosion() {
        soundManager.playExplode()
    }

    override fun playSoundCoin() {
        soundManager.playCoin()
    }

    override fun playSoundShoot() {
        soundManager.playShoot()
    }

    override fun playSoundUpgrade() {
        soundManager.playUpgrade()
    }

    // --- Cooldown UI Implementation (GameUiCallbacks) ---

    override fun onShootSuccess(cooldownMs: Long) {
        // Start animation from 0 (empty) to 10000 (full) over duration
        animateCooldown(cooldownMs)
    }

    override fun onShootFailed() {
        // Play locked sound
        soundManager.playLocked()

        // Shake animation for visual feedback
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

        // Cancel previous if running
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
        // Access the LayerDrawable and then the ClipDrawable inside it
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

            // Throttle sensor events
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

    // --- Game Pause & Dialog Logic ---

    private fun togglePauseByUser() {
        // 1. Pause the game engine
        controller.setPaused(true)
        updatePauseIcon()

        // 2. Stop loops and audio
        loop.stop()
        soundManager.pauseMusic()
        cooldownAnimator?.pause()

        // 3. Show the Custom Pause Dialog
        showPauseDialog()
    }

    private fun showPauseDialog() {
        // Create custom dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pause)
        // Make background transparent so our rounded drawable shows correctly
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false) // User must click a button

        // Bind Views in Dialog
        val seekMusic = dialog.findViewById<SeekBar>(R.id.seek_dialog_music)
        val seekSfx = dialog.findViewById<SeekBar>(R.id.seek_dialog_sfx)
        val btnResume = dialog.findViewById<Button>(R.id.btn_dialog_resume)
        val btnRestart = dialog.findViewById<Button>(R.id.btn_dialog_restart)
        val btnExit = dialog.findViewById<Button>(R.id.btn_dialog_exit)

        // Load current volume settings to set slider positions
        val settings = SettingsStorage(this).load()
        seekMusic.progress = settings.musicVolume
        seekSfx.progress = settings.sfxVolume

        val storage = SettingsStorage(this)

        // Real-time Music Volume Adjustment
        seekMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                soundManager.setVolumes(progress, seekSfx.progress)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Save setting when user releases slider
                storage.saveAudio(seekMusic.progress, seekSfx.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

        // Real-time SFX Volume Adjustment
        seekSfx.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                soundManager.setVolumes(seekMusic.progress, progress)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                storage.saveAudio(seekMusic.progress, seekSfx.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

        // Button Actions
        btnResume.setOnClickListener {
            dialog.dismiss()
            resumeGame()
        }

        btnRestart.setOnClickListener {
            dialog.dismiss()
            restartGameLogic()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            navigateHome()
        }

        dialog.show()
    }

    // Helper to resume game state
    private fun resumeGame() {
        controller.setPaused(false)
        updatePauseIcon()
        loop.start()
        soundManager.startMusic()
        cooldownAnimator?.resume()
    }

    // Helper to restart game state
    private fun restartGameLogic() {
        gameOverShown = false
        controller.resetGame()
        loop.start()
        soundManager.startMusic()
        updatePauseIcon()
        setButtonCooldownLevel(10000) // Reset button visual
    }

    // Helper to go home
    private fun navigateHome() {
        startActivity(Intent(this, StartMenuActivity::class.java))
        finish()
    }

    private fun updatePauseIcon() {
        // Icon logic: if paused -> show play icon (in HUD), else show pause icon
        btnPause.setImageResource(if (controller.isPaused()) R.drawable.ic_play else R.drawable.ic_pause)
    }

    // --- UI Helpers ---

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

    // --- Custom Game Over Dialog ---
    override fun showGameOverDialog(finalScore: Int) {
        gameOverShown = true
        loop.stop()
        soundManager.pauseMusic()
        cooldownAnimator?.cancel()

        // Create Custom Dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_game_over)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvScore = dialog.findViewById<TextView>(R.id.tv_final_score)
        val btnRestart = dialog.findViewById<Button>(R.id.btn_go_restart)
        val btnExit = dialog.findViewById<Button>(R.id.btn_go_exit)

        tvScore.text = finalScore.toString()

        btnRestart.setOnClickListener {
            dialog.dismiss()
            restartGameLogic()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            navigateHome()
        }

        dialog.show()
    }
}