// English comments only inside code
package com.rongo.carnumtwo.feature.game.model

data class GameState(
    val cols: Int,
    val rows: Int,
    var playerCol: Int,
    var paused: Boolean
)
