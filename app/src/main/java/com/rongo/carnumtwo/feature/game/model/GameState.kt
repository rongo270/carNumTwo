// English comments only inside code
package com.rongo.carnumtwo.feature.game.model

data class GameState(
    val cols: Int,
    val rows: Int,
    var playerCol: Int,
    var paused: Boolean,
    var lives: Int,
    var score: Int,
    var invulnerableUntilMs: Long,
    val chickens: MutableList<Chicken>,
    val bullets: MutableList<Bullet>,
    var lastShotAtMs: Long
) {
    fun isInvulnerable(nowMs: Long): Boolean = nowMs < invulnerableUntilMs
}
