package com.rongo.carnumtwo.feature.game.engine

import android.os.Handler
import android.os.Looper

/**
 * A dynamic game loop that adjusts its speed based on the controller.
 * Instead of fixed intervals, it schedules the next tick based on currentTickRate.
 */
class GameLoop(
    private val controller: GameController
) {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            if (!controller.isPaused()) {
                // Perform one master game step (movement, logic, etc.)
                controller.performGameStep()
            }

            // Get the current speed (which might have accelerated during the step)
            val nextDelay = controller.getCurrentTickRate()

            // Schedule the next loop iteration
            handler.postDelayed(this, nextDelay)
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        handler.post(loopRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}