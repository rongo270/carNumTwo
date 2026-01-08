package com.rongo.carnumtwo.core.storage

data class AppSettings(
    val gridX: Int,
    val gridY: Int,
    val tickMs: Long,
    val spawnMs: Long,
    val language: String,
    val enableButtons: Boolean,
    val enableTilt: Boolean
)