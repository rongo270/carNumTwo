package com.rongo.carnumtwo.feature.game.engine

import android.os.SystemClock
import com.rongo.carnumtwo.feature.game.model.Bullet
import com.rongo.carnumtwo.feature.game.model.GameState

class BulletManager(
    private val shotCooldownMs: Long
) {

    // Try to shoot a bullet if cooldown allows
    fun tryShoot(state: GameState, onChickenHit: () -> Unit) {
        if (state.paused) return

        val now = SystemClock.uptimeMillis()
        if (now - state.lastShotAtMs < shotCooldownMs) return
        state.lastShotAtMs = now

        // Bullet starts one row above the ship
        val startRow = state.rows - 2
        if (startRow < 0) return

        val col = state.playerCol

        // If chicken is directly above, remove it instantly
        val chickenIdx = findChickenIndex(state, startRow, col)
        if (chickenIdx != -1) {
            state.chickens.removeAt(chickenIdx)
            onChickenHit()
            return
        }

        // Prevent multiple bullets in the same starting cell
        val hasBullet = state.bullets.any { it.row == startRow && it.col == col }
        if (hasBullet) return

        state.bullets.add(Bullet(row = startRow, col = col))
    }

    // Move each bullet up by one cell and handle hits/off-board
    fun moveBulletsOneStep(state: GameState, onChickenHit: () -> Unit) {
        if (state.paused) return

        val it = state.bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            val nextRow = b.row - 1

            // Remove bullet if it leaves the board
            if (nextRow < 0) {
                it.remove()
                continue
            }

            // If bullet hits a chicken in the next cell, remove both
            val chickenIdx = findChickenIndex(state, nextRow, b.col)
            if (chickenIdx != -1) {
                state.chickens.removeAt(chickenIdx)
                it.remove()
                onChickenHit()
                continue
            }

            // Apply the movement
            b.row = nextRow
        }
    }

    // Find a chicken index at a given cell (row, col)
    private fun findChickenIndex(state: GameState, row: Int, col: Int): Int {
        return state.chickens.indexOfFirst { it.row == row && it.col == col }
    }
}
