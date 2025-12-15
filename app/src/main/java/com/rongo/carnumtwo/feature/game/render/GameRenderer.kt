// English comments only inside code
package com.rongo.carnumtwo.feature.game.render

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
        renderPlayer(state.playerCol)
    }

    private fun clearAllCells() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                cells[r][c].setImageDrawable(null)
            }
        }
    }

    private fun renderPlayer(playerCol: Int) {
        val bottomRow = rows - 1
        cells[bottomRow][playerCol].setImageResource(R.drawable.ic_spaceship)
    }
}
