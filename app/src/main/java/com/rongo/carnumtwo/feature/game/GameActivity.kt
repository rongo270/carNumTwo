package com.rongo.carnumtwo.feature.game

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.audio.SoundManager
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.core.storage.ScoreStorage
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

    // --- Game Engine Components ---
    private lateinit var controller: GameController
    private lateinit var renderer: GameRenderer
    private lateinit var loop: GameLoop

    private lateinit var state: GameState
    private lateinit var cells: Array<Array<ImageView>>

    // --- Managers ---
    private lateinit var soundManager: SoundManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // --- State Flags ---
    private var gameOverShown = false

    // Temp variables to hold data while picking location
    private var pendingScore: Int = 0
    private var pendingName: String = ""

    // --- Animation ---
    private var cooldownAnimator: ValueAnimator? = null

    // --- Sensors ---
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isTiltEnabled = false
    private var lastTiltMoveTime = 0L
    private val TILT_MOVE_COOLDOWN = 150L
    private val TILT_THRESHOLD = 1.5f

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // --- Activity Result Launcher for Map Picker (NEW) ---
    private val mapPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = result.data?.getDoubleExtra("lon", 0.0) ?: 0.0

            // Save the score with the picked location
            ScoreStorage(this).addScore(pendingName, pendingScore, lat, lon)

            // Restart game logic
            restartGameLogic()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        soundManager = SoundManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        btnFire.setBackgroundResource(R.drawable.bg_fire_button_cooldown)
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

        checkLocationPermission()

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
        cooldownAnimator?.cancel()

        if (isTiltEnabled) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loop.stop()
        soundManager.release()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun saveScoreWithGPS(name: String, score: Int) {
        val storage = ScoreStorage(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val lat = location?.latitude ?: 0.0
                val lon = location?.longitude ?: 0.0
                storage.addScore(name, score, lat, lon)
            }.addOnFailureListener {
                storage.addScore(name, score, 0.0, 0.0)
            }
        } else {
            storage.addScore(name, score, 0.0, 0.0)
        }
    }

    // --- Audio Callbacks ---
    override fun playSoundMove() { soundManager.playMove() }
    override fun playSoundExplosion() { soundManager.playExplode() }
    override fun playSoundCoin() { soundManager.playCoin() }
    override fun playSoundShoot() { soundManager.playShoot() }
    override fun playSoundUpgrade() { soundManager.playUpgrade() }

    // --- Cooldown UI Callbacks ---
    override fun onShootSuccess(cooldownMs: Long) { animateCooldown(cooldownMs) }
    override fun onShootFailed() {
        soundManager.playLocked()
        btnFire.animate().translationX(10f).setDuration(50).withEndAction {
            btnFire.animate().translationX(-10f).setDuration(50).withEndAction {
                btnFire.animate().translationX(0f).setDuration(50).start()
            }.start()
        }.start()
    }

    private fun animateCooldown(durationMs: Long) {
        if (durationMs <= 0) { setButtonCooldownLevel(10000); return }
        cooldownAnimator?.cancel()
        cooldownAnimator = ValueAnimator.ofInt(0, 10000).apply {
            duration = durationMs
            addUpdateListener { animator -> setButtonCooldownLevel(animator.animatedValue as Int) }
            start()
        }
    }

    private fun setButtonCooldownLevel(level: Int) {
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
                    controller.moveRight(); lastTiltMoveTime = now
                } else if (x > TILT_THRESHOLD) {
                    controller.moveLeft(); lastTiltMoveTime = now
                }
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    // --- Game Flow ---
    private fun togglePauseByUser() {
        controller.setPaused(true)
        updatePauseIcon()
        loop.stop()
        soundManager.pauseMusic()
        cooldownAnimator?.pause()
        showPauseDialog()
    }

    private fun showPauseDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pause)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val seekMusic = dialog.findViewById<SeekBar>(R.id.seek_dialog_music)
        val seekSfx = dialog.findViewById<SeekBar>(R.id.seek_dialog_sfx)
        val btnResume = dialog.findViewById<Button>(R.id.btn_dialog_resume)
        val btnRestart = dialog.findViewById<Button>(R.id.btn_dialog_restart)
        val btnExit = dialog.findViewById<Button>(R.id.btn_dialog_exit)

        val settings = SettingsStorage(this).load()
        seekMusic.progress = settings.musicVolume
        seekSfx.progress = settings.sfxVolume
        val storage = SettingsStorage(this)

        seekMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                soundManager.setVolumes(progress, seekSfx.progress)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                storage.saveAudio(seekMusic.progress, seekSfx.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

        seekSfx.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                soundManager.setVolumes(seekMusic.progress, progress)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                storage.saveAudio(seekMusic.progress, seekSfx.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

        btnResume.setOnClickListener { dialog.dismiss(); resumeGame() }
        btnRestart.setOnClickListener { dialog.dismiss(); restartGameLogic() }
        btnExit.setOnClickListener { dialog.dismiss(); navigateHome() }
        dialog.show()
    }

    private fun resumeGame() {
        controller.setPaused(false)
        updatePauseIcon()
        loop.start()
        soundManager.startMusic()
        cooldownAnimator?.resume()
    }

    private fun restartGameLogic() {
        gameOverShown = false
        controller.resetGame()
        loop.start()
        soundManager.startMusic()
        updatePauseIcon()
        setButtonCooldownLevel(10000)
    }

    private fun navigateHome() {
        startActivity(Intent(this, StartMenuActivity::class.java))
        finish()
    }

    private fun updatePauseIcon() {
        btnPause.setImageResource(if (controller.isPaused()) R.drawable.ic_play else R.drawable.ic_pause)
    }

    // --- High Score Logic (UPDATED) ---

    override fun showGameOverDialog(finalScore: Int) {
        gameOverShown = true
        loop.stop()
        soundManager.pauseMusic()
        cooldownAnimator?.cancel()

        val storage = ScoreStorage(this)

        if (storage.isHighScore(finalScore)) {
            showNewHighScoreDialog(finalScore)
        } else {
            showRegularGameOverDialog(finalScore)
        }
    }

    private fun showNewHighScoreDialog(score: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_new_high_score)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val etName = dialog.findViewById<EditText>(R.id.et_player_name)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save_score)
        val rbCurrent = dialog.findViewById<RadioButton>(R.id.rb_current_location)
        val rbPick = dialog.findViewById<RadioButton>(R.id.rb_pick_on_map)

        btnSave.setOnClickListener {
            val defaultName = getString(R.string.default_player_name)
            val name = etName.text.toString().ifEmpty { defaultName }

            if (rbPick.isChecked) {
                // *** CASE 1: Pick on Map ***
                pendingScore = score
                pendingName = name
                dialog.dismiss()
                val intent = Intent(this, LocationPickerActivity::class.java)
                mapPickerLauncher.launch(intent)

            } else if (rbCurrent.isChecked) {
                // *** CASE 2: Use GPS ***
                saveScoreWithGPS(name, score)
                dialog.dismiss()
                restartGameLogic()
            } else {
                // *** CASE 3: No Location ***
                ScoreStorage(this).addScore(name, score, 0.0, 0.0)
                dialog.dismiss()
                restartGameLogic()
            }
        }
        dialog.show()
    }

    private fun showRegularGameOverDialog(finalScore: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_game_over)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvScore = dialog.findViewById<TextView>(R.id.tv_final_score)
        val btnRestart = dialog.findViewById<Button>(R.id.btn_go_restart)
        val btnExit = dialog.findViewById<Button>(R.id.btn_go_exit)

        tvScore.text = finalScore.toString()

        btnRestart.setOnClickListener { dialog.dismiss(); restartGameLogic() }
        btnExit.setOnClickListener { dialog.dismiss(); navigateHome() }
        dialog.show()
    }

    // --- UI Setup ---
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
                grid.addView(cell)
                cell
            }
        }

        grid.post {
            val margin = dpToPx(6)
            val availableW = max(0, grid.width - (cols * margin * 2))
            val availableH = max(0, grid.height - (rows * margin * 2))
            val cellSize = max(1, min(if (cols > 0) availableW / cols else 0, if (rows > 0) availableH / rows else 0))

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

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun updateHearts(lives: Int) {
        setHeart(heart1, lives >= 1)
        setHeart(heart2, lives >= 2)
        setHeart(heart3, lives >= 3)
    }

    private fun setHeart(view: ImageView, isAlive: Boolean) {
        view.setImageResource(R.drawable.ic_heart_full)
        view.alpha = if (isAlive) 1f else 0.25f
    }

    override fun updateScore(score: Int) { tvScore.text = getString(R.string.score_label, score) }
    override fun updateCoins(coins: Int) { tvCoins.text = coins.toString() }

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
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
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
}