package com.rongo.carnumtwo.feature.game.model

data class Bullet(
    var row: Int,
    var col: Int,
    var power: Int // 1 to 5. Reduces by 1 when hitting a normal chicken.
)