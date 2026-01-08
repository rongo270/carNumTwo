package com.rongo.carnumtwo.feature.game.engine

import android.os.SystemClock
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.feature.game.model.Bullet
import com.rongo.carnumtwo.feature.game.model.GameState

class BulletManager(
    private val shotCooldownMs: Long
) {

    // Removed 'private' so Controller can check logic
    fun getCurrentPower(coins: Int): Int {
        return when {
            coins >= GameDefaults.COINS_FOR_LVL_5 -> 5
            coins >= GameDefaults.COINS_FOR_LVL_4 -> 4
            coins >= GameDefaults.COINS_FOR_LVL_3 -> 3
            coins >= GameDefaults.COINS_FOR_LVL_2 -> 2
            else -> 1
        }
    }

    fun tryShoot(state: GameState, onHit: () -> Unit): Boolean {
        if (state.paused) return false

        val now = SystemClock.uptimeMillis()

        if (now - state.lastShotAtMs < shotCooldownMs) return false

        val startRow = state.rows - 2
        if (startRow < 0) return false

        val col = state.playerCol
        val power = getCurrentPower(state.coinsCollected)

        // 1. Check spawn kill
        val chickenIdx = findChickenIndex(state, startRow, col)
        if (chickenIdx != -1) {
            state.chickens.removeAt(chickenIdx)
            onHit()
            if (power > 1) {
                state.bullets.add(Bullet(row = startRow, col = col, power = power - 1))
            }
            state.lastShotAtMs = now
            return true
        }

        // 2. Prevent stacking
        val hasBullet = state.bullets.any { it.row == startRow && it.col == col }
        if (hasBullet) return false

        // 3. Spawn Bullet
        state.bullets.add(Bullet(row = startRow, col = col, power = power))
        state.lastShotAtMs = now
        return true
    }

    fun moveBulletsOneStep(state: GameState, onHit: () -> Unit) {
        if (state.paused) return

        val it = state.bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            val nextRow = b.row - 1

            if (nextRow < 0) {
                it.remove()
                continue
            }

            val chickenIdx = findChickenIndex(state, nextRow, b.col)
            if (chickenIdx != -1) {
                state.chickens.removeAt(chickenIdx)
                onHit()

                b.power -= 1
                if (b.power <= 0) {
                    it.remove()
                } else {
                    b.row = nextRow
                }
                continue
            }

            b.row = nextRow
        }
    }

    private fun findChickenIndex(state: GameState, row: Int, col: Int): Int {
        return state.chickens.indexOfFirst { it.row == row && it.col == col }
    }
}