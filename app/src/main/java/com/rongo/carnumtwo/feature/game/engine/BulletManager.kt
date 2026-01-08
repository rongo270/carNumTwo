package com.rongo.carnumtwo.feature.game.engine

import android.os.SystemClock
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.feature.game.model.Bullet
import com.rongo.carnumtwo.feature.game.model.GameState

class BulletManager(
    private val shotCooldownMs: Long
) {

    // Calculate weapon power based on coins
    private fun getCurrentPower(coins: Int): Int {
        return when {
            coins >= GameDefaults.COINS_FOR_LVL_5 -> 5
            coins >= GameDefaults.COINS_FOR_LVL_4 -> 4
            coins >= GameDefaults.COINS_FOR_LVL_3 -> 3
            coins >= GameDefaults.COINS_FOR_LVL_2 -> 2
            else -> 1
        }
    }

    fun tryShoot(state: GameState, onHit: () -> Unit) {
        if (state.paused) return

        val now = SystemClock.uptimeMillis()
        if (now - state.lastShotAtMs < shotCooldownMs) return
        state.lastShotAtMs = now

        val startRow = state.rows - 2
        if (startRow < 0) return

        val col = state.playerCol
        val power = getCurrentPower(state.coinsCollected)

        // 1. Check if chicken is immediately above (Spawn Kill)
        val chickenIdx = findChickenIndex(state, startRow, col)
        if (chickenIdx != -1) {
            state.chickens.removeAt(chickenIdx)
            onHit()
            // Even if we kill instantly, if power > 1, the bullet continues as (power - 1)
            // But visually it spawns at startRow.
            if (power > 1) {
                state.bullets.add(Bullet(row = startRow, col = col, power = power - 1))
            }
            return
        }

        // 2. Prevent stacking bullets in the same exact cell
        val hasBullet = state.bullets.any { it.row == startRow && it.col == col }
        if (hasBullet) return

        // 3. Spawn Bullet
        state.bullets.add(Bullet(row = startRow, col = col, power = power))
    }

    fun moveBulletsOneStep(state: GameState, onHit: () -> Unit) {
        if (state.paused) return

        val it = state.bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            val nextRow = b.row - 1

            // 1. Check bounds
            if (nextRow < 0) {
                it.remove()
                continue
            }

            // 2. Check collision
            val chickenIdx = findChickenIndex(state, nextRow, b.col)
            if (chickenIdx != -1) {
                // Hit!
                state.chickens.removeAt(chickenIdx)
                onHit()

                // Degrade bullet
                b.power -= 1 // Assume normal chicken has 1 HP/Armor

                if (b.power <= 0) {
                    it.remove() // Bullet destroyed
                } else {
                    b.row = nextRow // Bullet continues moving
                }
                continue
            }

            // 3. Just move
            b.row = nextRow
        }
    }

    private fun findChickenIndex(state: GameState, row: Int, col: Int): Int {
        return state.chickens.indexOfFirst { it.row == row && it.col == col }
    }
}