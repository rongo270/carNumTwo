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
    fun updateCoins(coins: Int) // New callback for UI
    fun showHitFeedback()
    fun showGameOverDialog(finalScore: Int)
}

class GameController(
    private val state: GameState,
    private val renderer: GameRenderer,
    private val ui: GameUiCallbacks,
    private val invulnerableMs: Long,
    private val bulletManager: BulletManager
) {

    fun init() {
        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        ui.updateCoins(state.coinsCollected)
        renderer.render(state)
    }

    fun isPaused(): Boolean = state.paused

    fun setPaused(paused: Boolean) {
        state.paused = paused
    }

    fun resetGame() {
        state.paused = false
        state.lives = 3
        state.score = 0
        state.coinsCollected = 0 // Reset coins on full restart
        state.invulnerableUntilMs = 0L
        state.chickens.clear()
        state.bullets.clear()
        state.coins.clear()
        state.lastShotAtMs = 0L
        state.playerCol = state.cols / 2

        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        ui.updateCoins(state.coinsCollected)
        renderer.render(state)
    }

    fun shoot() {
        if (state.paused) return
        bulletManager.tryShoot(state) {
            // Logic for hit score if needed
        }
        renderer.render(state)
    }

    fun moveLeft() {
        if (state.paused) return
        if (state.playerCol > 0) {
            state.playerCol--
            checkPlayerCollisions()
            renderer.render(state)
        }
    }

    fun moveRight() {
        if (state.paused) return
        if (state.playerCol < state.cols - 1) {
            state.playerCol++
            checkPlayerCollisions()
            renderer.render(state)
        }
    }

    fun onTick() {
        if (state.paused) return

        // 1. Move bullets
        bulletManager.moveBulletsOneStep(state) {
            // Callback when chicken hit
        }

        val bottomRow = state.rows - 1
        val now = SystemClock.uptimeMillis()

        // 2. Move Chickens
        val itChickens = state.chickens.iterator()
        while (itChickens.hasNext()) {
            val ch = itChickens.next()
            ch.row += 1

            // Off board
            if (ch.row >= state.rows) {
                itChickens.remove()
                continue
            }

            // Hit Bullet?
            // Note: BulletManager handles bullet moving into chicken.
            // Here we handle chicken moving into bullet.
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

            // Hit Player?
            if (ch.row == bottomRow && ch.col == state.playerCol) {
                itChickens.remove()
                handleHit(now)
            }
        }

        // 3. Move Coins
        val itCoins = state.coins.iterator()
        while (itCoins.hasNext()) {
            val coin = itCoins.next()
            coin.row += 1

            // Off board
            if (coin.row >= state.rows) {
                itCoins.remove()
                continue
            }

            // Collected by Player?
            if (coin.row == bottomRow && coin.col == state.playerCol) {
                itCoins.remove()
                collectCoin()
            }
        }

        renderer.render(state)
    }

    fun onSpawn() {
        if (state.paused) return

        // Map occupied columns at row 0
        val occupiedTop = BooleanArray(state.cols)
        for (ch in state.chickens) {
            if (ch.row == 0 && ch.col in 0 until state.cols) occupiedTop[ch.col] = true
        }
        for (c in state.coins) {
            if (c.row == 0 && c.col in 0 until state.cols) occupiedTop[c.col] = true
        }

        val freeCols = mutableListOf<Int>()
        for (c in 0 until state.cols) if (!occupiedTop[c]) freeCols.add(c)

        if (freeCols.isEmpty()) return

        // Random logic
        val chosenCol = freeCols[Random.nextInt(freeCols.size)]

        // 5% chance to spawn Coin instead of Chicken
        // But only if we have free slots.
        // Note: You might want to allow both in one tick if multiple cols are free,
        // but let's keep it simple: either a chicken spawns OR a coin spawns (rarely) OR nothing.
        // Or better: Always spawn chicken, AND maybe spawn coin in ANOTHER slot.

        // Let's spawn Chicken first
        state.chickens.add(Chicken(row = 0, col = chosenCol))
        occupiedTop[chosenCol] = true // Mark as taken

        // Now try to spawn Coin
        if (Random.nextInt(100) < GameDefaults.COIN_SPAWN_CHANCE_PERCENT) {
            // Find free cols again
            freeCols.clear()
            for (c in 0 until state.cols) if (!occupiedTop[c]) freeCols.add(c)

            if (freeCols.isNotEmpty()) {
                val coinCol = freeCols[Random.nextInt(freeCols.size)]
                state.coins.add(Coin(row = 0, col = coinCol))
            }
        }

        renderer.render(state)
    }

    fun onScoreTick() {
        if (state.paused) return
        state.score += 1
        ui.updateScore(state.score)
    }

    private fun checkPlayerCollisions() {
        val bottomRow = state.rows - 1

        // Check Chicken
        val chIdx = state.chickens.indexOfFirst { it.row == bottomRow && it.col == state.playerCol }
        if (chIdx != -1) {
            state.chickens.removeAt(chIdx)
            handleHit(SystemClock.uptimeMillis())
        }

        // Check Coin
        val coinIdx = state.coins.indexOfFirst { it.row == bottomRow && it.col == state.playerCol }
        if (coinIdx != -1) {
            state.coins.removeAt(coinIdx)
            collectCoin()
        }
    }

    private fun collectCoin() {
        state.coinsCollected += 1
        ui.updateCoins(state.coinsCollected)
    }

    private fun handleHit(now: Long) {
        if (state.isInvulnerable(now)) return

        state.lives -= 1
        ui.updateHearts(state.lives)
        ui.showHitFeedback()

        // PENALTY: Lose 25 coins
        val oldCoins = state.coinsCollected
        state.coinsCollected = max(0, state.coinsCollected - GameDefaults.PENALTY_ON_DEATH)

        if (oldCoins != state.coinsCollected) {
            ui.updateCoins(state.coinsCollected)
        }

        state.invulnerableUntilMs = now + invulnerableMs

        if (state.lives <= 0) {
            state.paused = true
            ui.showGameOverDialog(state.score)
        }
    }
}