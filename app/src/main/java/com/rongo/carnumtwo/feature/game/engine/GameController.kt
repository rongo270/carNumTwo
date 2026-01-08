package com.rongo.carnumtwo.feature.game.engine

import android.os.SystemClock
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.feature.game.model.Chicken
import com.rongo.carnumtwo.feature.game.model.Coin
import com.rongo.carnumtwo.feature.game.model.GameState
import com.rongo.carnumtwo.feature.game.render.GameRenderer
import kotlin.math.max
import kotlin.random.Random

interface GameUiCallbacks {
    fun updateHearts(lives: Int)
    fun updateScore(score: Int)
    fun updateCoins(coins: Int)
    fun showHitFeedback()
    fun showGameOverDialog(finalScore: Int)

    // Audio Callbacks
    fun playSoundMove()
    fun playSoundExplosion()
    fun playSoundCoin()

    // Shoot Cooldown Callbacks
    fun onShootSuccess(cooldownMs: Long)
    fun onShootFailed()
}

class GameController(
    private val state: GameState,
    private val renderer: GameRenderer,
    private val ui: GameUiCallbacks,
    private val invulnerableMs: Long,
    private val bulletManager: BulletManager,
    private val initialTickMs: Long
) {

    private var currentTickMs: Long = initialTickMs
    private val minTickMs: Long = 150L
    private val accelerationFactor: Float = 0.003f
    private var tickCounter: Int = 0

    fun init() {
        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        ui.updateCoins(state.coinsCollected)
        // Ensure button starts full (ready)
        ui.onShootSuccess(0)
        renderer.render(state)
    }

    fun getCurrentTickRate(): Long {
        return if (state.paused) 100L else currentTickMs
    }

    fun isPaused(): Boolean = state.paused

    fun setPaused(paused: Boolean) {
        state.paused = paused
    }

    fun resetGame() {
        state.paused = false
        state.lives = 3
        state.score = 0
        state.coinsCollected = 0
        state.invulnerableUntilMs = 0L
        state.chickens.clear()
        state.bullets.clear()
        state.coins.clear()
        state.lastShotAtMs = 0L
        state.playerCol = state.cols / 2

        currentTickMs = initialTickMs
        tickCounter = 0

        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        ui.updateCoins(state.coinsCollected)
        // Reset cooldown visual
        ui.onShootSuccess(0)
        renderer.render(state)
    }

    fun performGameStep() {
        if (state.paused) return

        moveBullets()
        moveEnemiesAndCoins()

        tickCounter++
        if (tickCounter % 2 == 0) {
            spawnEnemyOrCoin()
            increaseScore()
            recalculateSpeed()
        }

        renderer.render(state)
    }

    // --- Private Logic ---

    private fun recalculateSpeed() {
        val speedMultiplier = 1f + (state.score * accelerationFactor)
        val newTick = (initialTickMs / speedMultiplier).toLong()
        currentTickMs = max(minTickMs, newTick)
    }

    private fun increaseScore() {
        state.score += 1
        ui.updateScore(state.score)
    }

    private fun spawnEnemyOrCoin() {
        val occupiedTop = BooleanArray(state.cols)
        for (ch in state.chickens) if (ch.row == 0 && ch.col in 0 until state.cols) occupiedTop[ch.col] = true
        for (c in state.coins) if (c.row == 0 && c.col in 0 until state.cols) occupiedTop[c.col] = true

        val freeCols = mutableListOf<Int>()
        for (c in 0 until state.cols) if (!occupiedTop[c]) freeCols.add(c)

        if (freeCols.isEmpty()) return

        val chosenCol = freeCols[Random.nextInt(freeCols.size)]
        state.chickens.add(Chicken(row = 0, col = chosenCol))
        occupiedTop[chosenCol] = true

        if (Random.nextInt(100) < GameDefaults.COIN_SPAWN_CHANCE_PERCENT) {
            freeCols.clear()
            for (c in 0 until state.cols) if (!occupiedTop[c]) freeCols.add(c)
            if (freeCols.isNotEmpty()) {
                val coinCol = freeCols[Random.nextInt(freeCols.size)]
                state.coins.add(Coin(row = 0, col = coinCol))
            }
        }
    }

    private fun moveBullets() {
        bulletManager.moveBulletsOneStep(state) { }
    }

    private fun moveEnemiesAndCoins() {
        val bottomRow = state.rows - 1
        val now = SystemClock.uptimeMillis()

        val itChickens = state.chickens.iterator()
        while (itChickens.hasNext()) {
            val ch = itChickens.next()
            ch.row += 1

            if (ch.row >= state.rows) {
                itChickens.remove()
                continue
            }

            val bulletIdx = state.bullets.indexOfFirst { b -> b.row == ch.row && b.col == ch.col }
            if (bulletIdx != -1) {
                val bullet = state.bullets[bulletIdx]
                bullet.power -= 1
                if (bullet.power <= 0) {
                    state.bullets.removeAt(bulletIdx)
                }
                itChickens.remove()
                continue
            }

            if (ch.row == bottomRow && ch.col == state.playerCol) {
                itChickens.remove()
                handleHit(now)
            }
        }

        val itCoins = state.coins.iterator()
        while (itCoins.hasNext()) {
            val coin = itCoins.next()
            coin.row += 1

            if (coin.row >= state.rows) {
                itCoins.remove()
                continue
            }

            if (coin.row == bottomRow && coin.col == state.playerCol) {
                itCoins.remove()
                collectCoin()
            }
        }
    }

    // --- Player Actions ---

    fun shoot() {
        if (state.paused) return

        // Check if shot was allowed
        val shotFired = bulletManager.tryShoot(state) { }

        if (shotFired) {
            ui.onShootSuccess(GameDefaults.SHOOT_COOLDOWN_MS)
        } else {
            ui.onShootFailed()
        }

        renderer.render(state)
    }

    fun moveLeft() {
        if (state.paused) return
        if (state.playerCol > 0) {
            state.playerCol--
            if (!checkPlayerCollisions()) ui.playSoundMove()
            renderer.render(state)
        }
    }

    fun moveRight() {
        if (state.paused) return
        if (state.playerCol < state.cols - 1) {
            state.playerCol++
            if (!checkPlayerCollisions()) ui.playSoundMove()
            renderer.render(state)
        }
    }

    private fun checkPlayerCollisions(): Boolean {
        val bottomRow = state.rows - 1
        var hitOccurred = false

        val chIdx = state.chickens.indexOfFirst { it.row == bottomRow && it.col == state.playerCol }
        if (chIdx != -1) {
            state.chickens.removeAt(chIdx)
            handleHit(SystemClock.uptimeMillis())
            hitOccurred = true
        }

        val coinIdx = state.coins.indexOfFirst { it.row == bottomRow && it.col == state.playerCol }
        if (coinIdx != -1) {
            state.coins.removeAt(coinIdx)
            collectCoin()
        }
        return hitOccurred
    }

    private fun collectCoin() {
        state.coinsCollected += 1
        ui.updateCoins(state.coinsCollected)
        ui.playSoundCoin()
    }

    private fun handleHit(now: Long) {
        if (state.isInvulnerable(now)) return

        state.lives -= 1
        ui.updateHearts(state.lives)
        ui.showHitFeedback()
        ui.playSoundExplosion()

        val oldCoins = state.coinsCollected
        state.coinsCollected = max(0, state.coinsCollected - GameDefaults.PENALTY_ON_DEATH)
        if (oldCoins != state.coinsCollected) ui.updateCoins(state.coinsCollected)

        state.invulnerableUntilMs = now + invulnerableMs

        if (state.lives <= 0) {
            state.paused = true
            ui.showGameOverDialog(state.score)
        }
    }
}