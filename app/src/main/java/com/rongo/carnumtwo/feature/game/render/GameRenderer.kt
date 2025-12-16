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

    // Render the current state into the grid cells
    fun render(state: GameState) {
        val now = SystemClock.uptimeMillis()

        // Clear the board (reset images and alpha)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = cells[r][c]
                cell.setImageResource(0)
                cell.alpha = 1f
            }
        }

        // Draw chickens
        for (ch in state.chickens) {
            if (ch.row in 0 until rows && ch.col in 0 until cols) {
                val cell = cells[ch.row][ch.col]
                cell.setImageResource(R.drawable.ic_chicken)
                cell.alpha = 1f
            }
        }

        // Draw bullets
        for (b in state.bullets) {
            if (b.row in 0 until rows && b.col in 0 until cols) {
                val cell = cells[b.row][b.col]
                cell.setImageResource(R.drawable.ic_bullet)
                cell.alpha = 1f
            }
        }

        // Draw player ship, with fade effect during invulnerability
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

    // Compute player alpha based on invulnerability window (fade out then in)
    private fun playerAlpha(
        nowMs: Long,
        invulnerableUntilMs: Long,
        invulnerableMs: Long
    ): Float {
        if (invulnerableMs <= 0L) return 1f

        val startMs = invulnerableUntilMs - invulnerableMs
        if (nowMs < startMs || nowMs >= invulnerableUntilMs) return 1f

        val t = ((nowMs - startMs).toFloat() / invulnerableMs.toFloat()).coerceIn(0f, 1f)

        // Fade down to min alpha, then back to full alpha
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
