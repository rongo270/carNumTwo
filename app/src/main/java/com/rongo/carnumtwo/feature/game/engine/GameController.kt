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
    private val invulnerableMs: Long,
    private val bulletManager: BulletManager
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
        state.bullets.clear()
        state.lastShotAtMs = 0L
        state.playerCol = state.cols / 2

        ui.updateHearts(state.lives)
        ui.updateScore(state.score)
        renderer.render(state)
    }

    fun shoot() {
        if (state.paused) return
        bulletManager.tryShoot(state) {
            // No score bonus yet (easy to add later)
        }
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

        // 1) Move bullets first
        bulletManager.moveBulletsOneStep(state) {
            // No score bonus yet (easy to add later)
        }

        // 2) Move chickens down
        val bottomRow = state.rows - 1
        val now = SystemClock.uptimeMillis()

        val it = state.chickens.iterator()
        while (it.hasNext()) {
            val ch = it.next()
            ch.row += 1

            // Fell off board
            if (ch.row >= state.rows) {
                it.remove()
                continue
            }

            // FIX: If chicken moved into a cell that currently has a bullet, remove both
            val bulletIdx = state.bullets.indexOfFirst { b -> b.row == ch.row && b.col == ch.col }
            if (bulletIdx != -1) {
                state.bullets.removeAt(bulletIdx)
                it.remove()
                continue
            }

            // Collision with ship
            if (ch.row == bottomRow && ch.col == state.playerCol) {
                it.remove()
                handleHit(now)
            }
        }

        renderer.render(state)
    }

    fun onSpawn() {
        if (state.paused) return

        val occupiedTop = BooleanArray(state.cols)
        for (ch in state.chickens) {
            if (ch.row == 0 && ch.col in 0 until state.cols) {
                occupiedTop[ch.col] = true
            }
        }

        val freeCols = mutableListOf<Int>()
        for (c in 0 until state.cols) if (!occupiedTop[c]) freeCols.add(c)
        if (freeCols.isEmpty()) return

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
        if (state.isInvulnerable(now)) return

        state.lives -= 1
        ui.updateHearts(state.lives)
        ui.showHitFeedback()

        state.invulnerableUntilMs = now + invulnerableMs

        if (state.lives <= 0) {
            state.paused = true
            ui.showGameOverDialog(state.score)
        }
    }
}
