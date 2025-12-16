package com.rongo.carnumtwo.feature.game.engine

import android.os.Handler
import android.os.Looper

class GameLoop(
    private val tickMs: Long,
    private val spawnMs: Long,
    private val controller: GameController
) {

    private val handler = Handler(Looper.getMainLooper())

    // Runs game tick updates (movement/collisions)
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!controller.isPaused()) controller.onTick()
            handler.postDelayed(this, tickMs)
        }
    }

    // Runs spawn updates (new chickens)
    private val spawnRunnable = object : Runnable {
        override fun run() {
            if (!controller.isPaused()) controller.onSpawn()
            handler.postDelayed(this, spawnMs)
        }
    }

    // Runs score updates (once per second)
    private val scoreRunnable = object : Runnable {
        override fun run() {
            if (!controller.isPaused()) controller.onScoreTick()
            handler.postDelayed(this, 1000L)
        }
    }

    // Start the loop (posts all runnables)
    fun start() {
        stop()
        handler.post(tickRunnable)
        handler.post(spawnRunnable)
        handler.post(scoreRunnable)
    }

    // Stop the loop (remove all scheduled callbacks)
    fun stop() {
        handler.removeCallbacksAndMessages(null)
    }
}
