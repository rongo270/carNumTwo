package com.rongo.carnumtwo.feature.game.render

import android.os.SystemClock
import android.widget.ImageView
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.config.GameDefaults
import com.rongo.carnumtwo.feature.game.model.GameState

class GameRenderer(
    private val cells: Array<Array<ImageView>>,
    private val rows: Int,
    private val cols: Int
) {

    fun render(state: GameState) {
        val now = SystemClock.uptimeMillis()

        // Clear grid
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = cells[r][c]
                cell.setImageResource(0)
                cell.alpha = 1f
            }
        }

        // Draw Coins
        for (coin in state.coins) {
            if (coin.row in 0 until rows && coin.col in 0 until cols) {
                cells[coin.row][coin.col].setImageResource(R.drawable.ic_coin)
            }
        }

        // Draw Chickens
        for (ch in state.chickens) {
            if (ch.row in 0 until rows && ch.col in 0 until cols) {
                cells[ch.row][ch.col].setImageResource(R.drawable.ic_chicken)
            }
        }

        // Draw Bullets (With different images based on power)
        for (b in state.bullets) {
            if (b.row in 0 until rows && b.col in 0 until cols) {
                val cell = cells[b.row][b.col]

                val resId = when(b.power) {
                    1 -> R.drawable.ic_bullet_1
                    2 -> R.drawable.ic_bullet_2
                    3 -> R.drawable.ic_bullet_3
                    4 -> R.drawable.ic_bullet_4
                    5 -> R.drawable.ic_bullet_5
                    else -> R.drawable.ic_bullet_1
                }

                cell.setImageResource(resId)
            }
        }

        // Draw Player
        val bottomRow = rows - 1
        if (bottomRow in 0 until rows && state.playerCol in 0 until cols) {
            val shipCell = cells[bottomRow][state.playerCol]
            shipCell.setImageResource(R.drawable.ic_spaceship)
            shipCell.alpha = playerAlpha(
                nowMs = now,
                invulnerableUntilMs = state.invulnerableUntilMs,
                invulnerableMs = GameDefaults.INVULNERABLE_MS
            )
        }
    }

    private fun playerAlpha(nowMs: Long, invulnerableUntilMs: Long, invulnerableMs: Long): Float {
        if (invulnerableMs <= 0L) return 1f
        val startMs = invulnerableUntilMs - invulnerableMs
        if (nowMs < startMs || nowMs >= invulnerableUntilMs) return 1f

        val t = ((nowMs - startMs).toFloat() / invulnerableMs.toFloat()).coerceIn(0f, 1f)
        val minAlpha = 0.25f
        return if (t < 0.5f) {
            val k = t / 0.5f
            1f - k * (1f - minAlpha)
        } else {
            val k = (t - 0.5f) / 0.5f
            minAlpha + k * (1f - minAlpha)
        }
    }
}