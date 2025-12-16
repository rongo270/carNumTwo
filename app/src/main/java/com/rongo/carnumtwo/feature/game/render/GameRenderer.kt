// English comments only inside code
package com.rongo.carnumtwo.feature.game.render

import android.os.SystemClock
import android.widget.ImageView
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.feature.game.model.GameState

class GameRenderer(
    private val cells: Array<Array<ImageView>>,
    private val rows: Int,
    private val cols: Int
) {

    fun render(state: GameState) {
        clearAllCells()
        renderBullets(state)
        renderChickens(state)
        renderPlayer(state)
    }

    private fun clearAllCells() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                cells[r][c].alpha = 1f
                cells[r][c].setImageDrawable(null)
            }
        }
    }

    private fun renderBullets(state: GameState) {
        for (b in state.bullets) {
            if (b.row in 0 until rows && b.col in 0 until cols) {
                cells[b.row][b.col].setImageResource(R.drawable.ic_bullet)
            }
        }
    }

    private fun renderChickens(state: GameState) {
        for (ch in state.chickens) {
            if (ch.row in 0 until rows && ch.col in 0 until cols) {
                cells[ch.row][ch.col].setImageResource(R.drawable.ic_chicken)
            }
        }
    }

    private fun renderPlayer(state: GameState) {
        val bottomRow = rows - 1
        val cell = cells[bottomRow][state.playerCol]
        cell.setImageResource(R.drawable.ic_spaceship)

        val now = SystemClock.uptimeMillis()
        if (state.isInvulnerable(now)) {
            val blink = ((now / 150L) % 2L == 0L)
            cell.alpha = if (blink) 0.25f else 1f
        } else {
            cell.alpha = 1f
        }
    }
}
