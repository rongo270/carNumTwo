// English comments only inside code
package com.rongo.carnumtwo.feature.game.engine

import com.rongo.carnumtwo.feature.game.model.GameState
import com.rongo.carnumtwo.feature.game.render.GameRenderer

class GameController(
    private val state: GameState,
    private val renderer: GameRenderer
) {

    fun init() {
        renderer.render(state)
    }

    fun moveLeft() {
        if (state.paused) return
        if (state.playerCol > 0) {
            state.playerCol--
            renderer.render(state)
        }
    }

    fun moveRight() {
        if (state.paused) return
        if (state.playerCol < state.cols - 1) {
            state.playerCol++
            renderer.render(state)
        }
    }

    fun togglePause() {
        state.paused = !state.paused
    }

    fun isPaused(): Boolean = state.paused
}
