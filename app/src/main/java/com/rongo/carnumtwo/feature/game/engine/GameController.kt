// English comments only inside code
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
    private val invulnerableMs: Long
) {

    fun init() {
        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
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
        state.invulnerableUntilMs = 0L
        state.chickens.clear()
        state.playerCol = state.cols / 2

        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        renderer.render(state)
    }

    fun moveLeft() {
        if (state.paused) return
        if (state.playerCol > 0) {
            state.playerCol--
            checkCollisionByMovement()
            renderer.render(state)
        }
    }

    fun moveRight() {
        if (state.paused) return
        if (state.playerCol < state.cols - 1) {
            state.playerCol++
            checkCollisionByMovement()
            renderer.render(state)
        }
    }

    fun onTick() {
        if (state.paused) return

        val bottomRow = state.rows - 1
        val now = SystemClock.uptimeMillis()

        // Move chickens down
        val it = state.chickens.iterator()
        while (it.hasNext()) {
            val ch = it.next()
            ch.row += 1

            // Fell off board
            if (ch.row >= state.rows) {
                it.remove()
                continue
            }

            // Collision by falling onto the spaceship cell
            if (ch.row == bottomRow && ch.col == state.playerCol) {
                it.remove()
                handleHit(now)
            }
        }

        renderer.render(state)
    }

    fun onSpawn() {
        if (state.paused) return

        // Spawn at top row with a simple "avoid same cell at row 0" rule
        val occupiedTop = BooleanArray(state.cols)
        for (ch in state.chickens) {
            if (ch.row == 0 && ch.col in 0 until state.cols) {
                occupiedTop[ch.col] = true
            }
        }

        val freeCols = mutableListOf<Int>()
        for (c in 0 until state.cols) {
            if (!occupiedTop[c]) freeCols.add(c)
        }
        if (freeCols.isEmpty()) {
            return
        }

        val col = freeCols[Random.nextInt(freeCols.size)]
        state.chickens.add(Chicken(row = 0, col = col))

        renderer.render(state)
    }

    fun onScoreTick() {
        if (state.paused) return
        state.score += 1
        ui.updateScore(state.score)
    }

    private fun checkCollisionByMovement() {
        val bottomRow = state.rows - 1
        val idx = state.chickens.indexOfFirst { it.row == bottomRow && it.col == state.playerCol }
        if (idx != -1) {
            state.chickens.removeAt(idx)
            val now = SystemClock.uptimeMillis()
            handleHit(now)
        }
    }

    private fun handleHit(now: Long) {
        // Chicken always disappears (already removed by caller)

        if (state.isInvulnerable(now)) {
            // No life loss during invulnerability
            return
        }

        state.lives -= 1
        ui.updateHearts(state.lives)
        ui.showHitFeedback()

        // Start invulnerability
        state.invulnerableUntilMs = now + invulnerableMs

        if (state.lives <= 0) {
            state.paused = true
            ui.showGameOverDialog(state.score)
        }
    }
}
