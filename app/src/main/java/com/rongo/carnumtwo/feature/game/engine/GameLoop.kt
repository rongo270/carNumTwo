// English comments only inside code
package com.rongo.carnumtwo.feature.game.engine

import android.os.Handler
import android.os.Looper

class GameLoop(
    private val tickMs: Long,
    private val spawnMs: Long,
    private val controller: GameController
) {

    private val handler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!controller.isPaused()) controller.onTick()
            handler.postDelayed(this, tickMs)
        }
    }

    private val spawnRunnable = object : Runnable {
        override fun run() {
            if (!controller.isPaused()) controller.onSpawn()
            handler.postDelayed(this, spawnMs)
        }
    }

    private val scoreRunnable = object : Runnable {
        override fun run() {
            if (!controller.isPaused()) controller.onScoreTick()
            handler.postDelayed(this, 1000L)
        }
    }

    fun start() {
        stop()
        handler.post(tickRunnable)
        handler.post(spawnRunnable)
        handler.post(scoreRunnable)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
    }
}
