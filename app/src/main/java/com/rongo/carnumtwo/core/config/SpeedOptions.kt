package com.rongo.carnumtwo.core.config

object SpeedOptions {

    // --- SLOW MODE ---
    const val SLOW_TICK_MS = 600L
    const val SLOW_SPAWN_MS = 1500L

    // --- MEDIUM MODE ---
    const val MEDIUM_TICK_MS = 400L
    const val MEDIUM_SPAWN_MS = 1000L

    // --- FAST MODE ---
    const val FAST_TICK_MS = 200L
    const val FAST_SPAWN_MS = 600L

    enum class SpeedLevel {
        SLOW, MEDIUM, FAST, CUSTOM
    }

    // Helper to determine current level based on tick value
    fun getLevel(tickMs: Long): SpeedLevel {
        return when (tickMs) {
            SLOW_TICK_MS -> SpeedLevel.SLOW
            MEDIUM_TICK_MS -> SpeedLevel.MEDIUM
            FAST_TICK_MS -> SpeedLevel.FAST
            else -> SpeedLevel.CUSTOM // Fallback if settings don't match presets
        }
    }
}