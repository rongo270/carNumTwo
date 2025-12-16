// English comments only inside code
package com.rongo.carnumtwo.core.config

object SpeedOptions {

    // Chicken moves down every tick
    val TICK_OPTIONS_MS = longArrayOf(
        200L, 300L, 400L, 500L, 600L, 700L, 800L
    )

    // New chicken spawns every spawn interval
    val SPAWN_OPTIONS_MS = longArrayOf(
        600L, 800L, 1000L, 1200L, 1500L, 2000L
    )
}
