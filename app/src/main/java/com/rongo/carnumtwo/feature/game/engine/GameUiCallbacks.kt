package com.rongo.carnumtwo.feature.game.engine

interface GameUiCallbacks {
    fun updateHearts(lives: Int)
    fun updateScore(score: Int)
    fun updateCoins(coins: Int)
    fun showHitFeedback()
    fun showGameOverDialog(finalScore: Int)

    // Audio Callbacks
    fun playSoundMove()
    fun playSoundExplosion()
    fun playSoundCoin()
    fun playSoundShoot()   // NEW
    fun playSoundUpgrade() // NEW

    // Shoot Cooldown Callbacks
    fun onShootSuccess(cooldownMs: Long)
    fun onShootFailed()
}