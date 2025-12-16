package com.rongo.carnumtwo.feature.game.engine

import android.os.SystemClock
import com.rongo.carnumtwo.feature.game.model.Chicken
import com.rongo.carnumtwo.feature.game.model.GameState
import com.rongo.carnumtwo.feature.game.render.GameRenderer
import kotlin.random.Random

interface GameUiCallbacks {
    fun updateHearts(lives: Int)
    fun updateScore(score: Int)
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

    // Initialize UI and render the starting state
    fun init() {
        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        renderer.render(state)
    }

    // Return whether the game is currently paused
    fun isPaused(): Boolean = state.paused

    // Set paused state (used by UI and loop)
    fun setPaused(paused: Boolean) {
        state.paused = paused
    }

    // Reset the game state back to the initial values
    fun resetGame() {
        state.paused = false
        state.lives = 3
        state.score = 0
        state.invulnerableUntilMs = 0L
        state.chickens.clear()
        state.bullets.clear()
        state.lastShotAtMs = 0L
        state.playerCol = state.cols / 2

        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        renderer.render(state)
    }

    // Handle shoot button click
    fun shoot() {
        if (state.paused) return
        bulletManager.tryShoot(state) {
            // Chicken removed by shot (score logic can be added later)
        }
        renderer.render(state)
    }

    // Move the player one cell left (if possible)
    fun moveLeft() {
        if (state.paused) return
        if (state.playerCol > 0) {
            state.playerCol--
            checkCollisionByMovement()
            renderer.render(state)
        }
    }

    // Move the player one cell right (if possible)
    fun moveRight() {
        if (state.paused) return
        if (state.playerCol < state.cols - 1) {
            state.playerCol++
            checkCollisionByMovement()
            renderer.render(state)
        }
    }

    // Called by the loop each "tick" to update gameplay
    fun onTick() {
        if (state.paused) return

        // 1) Move bullets first
        bulletManager.moveBulletsOneStep(state) {
            // Chicken removed by bullet movement (score logic can be added later)
        }

        // 2) Move chickens down
        val bottomRow = state.rows - 1
        val now = SystemClock.uptimeMillis()

        val it = state.chickens.iterator()
        while (it.hasNext()) {
            val ch = it.next()
            ch.row += 1

            // Remove chicken if it goes off the board
            if (ch.row >= state.rows) {
                it.remove()
                continue
            }

            // If chicken moved into a bullet cell, remove both
            val bulletIdx = state.bullets.indexOfFirst { b -> b.row == ch.row && b.col == ch.col }
            if (bulletIdx != -1) {
                state.bullets.removeAt(bulletIdx)
                it.remove()
                continue
            }

            // If chicken hits the ship, handle hit
            if (ch.row == bottomRow && ch.col == state.playerCol) {
                it.remove()
                handleHit(now)
            }
        }

        renderer.render(state)
    }

    // Called by the loop to spawn a new chicken at the top row
    fun onSpawn() {
        if (state.paused) return

        // Track columns already occupied at row 0
        val occupiedTop = BooleanArray(state.cols)
        for (ch in state.chickens) {
            if (ch.row == 0 && ch.col in 0 until state.cols) {
                occupiedTop[ch.col] = true
            }
        }

        // Pick a random free column at the top
        val freeCols = mutableListOf<Int>()
        for (c in 0 until state.cols) if (!occupiedTop[c]) freeCols.add(c)
        if (freeCols.isEmpty()) return

        val col = freeCols[Random.nextInt(freeCols.size)]
        state.chickens.add(Chicken(row = 0, col = col))

        renderer.render(state)
    }

    // Called every second to increase score over time
    fun onScoreTick() {
        if (state.paused) return
        state.score += 1
        ui.updateScore(state.score)
    }

    // Check collision after player movement (player moved into chicken)
    private fun checkCollisionByMovement() {
        val bottomRow = state.rows - 1
        val idx = state.chickens.indexOfFirst { it.row == bottomRow && it.col == state.playerCol }
        if (idx != -1) {
            state.chickens.removeAt(idx)
            val now = SystemClock.uptimeMillis()
            handleHit(now)
        }
    }

    // Handle a player hit (reduce life, set invulnerability, show game over if needed)
    private fun handleHit(now: Long) {
        if (state.isInvulnerable(now)) return

        state.lives -= 1
        ui.updateHearts(state.lives)
        ui.showHitFeedback()

        // Start invulnerability window
        state.invulnerableUntilMs = now + invulnerableMs

        // If no lives left, stop and show game over
        if (state.lives <= 0) {
            state.paused = true
            ui.showGameOverDialog(state.score)
        }
    }
}
