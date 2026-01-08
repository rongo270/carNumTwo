package com.rongo.carnumtwo.core.config

object GameDefaults {
    const val DEFAULT_GRID_X = 5
    const val DEFAULT_GRID_Y = 10

    const val MIN_GRID_X = 3
    const val MAX_GRID_X = 7
    const val MIN_GRID_Y = 5
    const val MAX_GRID_Y = 12

    const val DEFAULT_TICK_MS = 500L
    const val DEFAULT_SPAWN_MS = 1000L

    const val INVULNERABLE_MS = 3000L

    const val DEFAULT_LANGUAGE = "en"

    // --- Coin & Weapon Logic ---
    const val COIN_SPAWN_CHANCE_PERCENT = 5
    const val PENALTY_ON_DEATH = 25

    // Weapon Levels based on coins
    const val COINS_FOR_LVL_2 = 15
    const val COINS_FOR_LVL_3 = 50
    const val COINS_FOR_LVL_4 = 100
    const val COINS_FOR_LVL_5 = 200

    // --- Cooldown Logic ---
    const val SHOOT_COOLDOWN_MS = 3000L // 3 Seconds cooldown
}