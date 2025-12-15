package com.rongo.carnumtwo.core.config

object GameDefaults {
    const val DEFAULT_GRID_X = 3
    const val DEFAULT_GRID_Y = 7

    const val MIN_GRID_X = 3
    const val MAX_GRID_X = 7
    const val MIN_GRID_Y = 5
    const val MAX_GRID_Y = 12

    const val DEFAULT_TICK_MS = 500L    // Chicken moves down every 0.5s
    const val DEFAULT_SPAWN_MS = 1000L  // New chicken spawns every 1s

    const val INVULNERABLE_MS = 1500L   // Spaceship blink duration
}
