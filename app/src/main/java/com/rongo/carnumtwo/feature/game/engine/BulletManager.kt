// English comments only inside code
package com.rongo.carnumtwo.feature.game.engine

import android.os.SystemClock
import com.rongo.carnumtwo.feature.game.model.Bullet
import com.rongo.carnumtwo.feature.game.model.GameState

class BulletManager(
    private val shotCooldownMs: Long
) {

    fun tryShoot(state: GameState, onChickenHit: () -> Unit) {
        if (state.paused) return

        val now = SystemClock.uptimeMillis()
        if (now - state.lastShotAtMs < shotCooldownMs) return
        state.lastShotAtMs = now

        val startRow = state.rows - 2
        if (startRow < 0) return

        val col = state.playerCol

        // If a chicken is directly above the ship, remove it immediately (no bullet needed)
        val chickenIdx = findChickenIndex(state, startRow, col)
        if (chickenIdx != -1) {
            state.chickens.removeAt(chickenIdx)
            onChickenHit()
            return
        }

        // Avoid stacking bullets in the same cell
        val hasBullet = state.bullets.any { it.row == startRow && it.col == col }
        if (hasBullet) return

        state.bullets.add(Bullet(row = startRow, col = col))
    }

    fun moveBulletsOneStep(state: GameState, onChickenHit: () -> Unit) {
        if (state.paused) return

        val it = state.bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            val nextRow = b.row - 1

            // Off the board
            if (nextRow < 0) {
                it.remove()
                continue
            }

            // Hit chicken in the next cell (covers "crossing" cases cleanly because bullets move first)
            val chickenIdx = findChickenIndex(state, nextRow, b.col)
            if (chickenIdx != -1) {
                state.chickens.removeAt(chickenIdx)
                it.remove()
                onChickenHit()
                continue
            }

            // Move bullet up
            b.row = nextRow
        }
    }

    private fun findChickenIndex(state: GameState, row: Int, col: Int): Int {
        return state.chickens.indexOfFirst { it.row == row && it.col == col }
    }
}
