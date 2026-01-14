package com.rongo.carnumtwo.feature.score

data class ScoreItem(
    val name: String, // Player Name
    val score: Int,
    val lat: Double,
    val lon: Double,
    val timestamp: Long = System.currentTimeMillis()
)