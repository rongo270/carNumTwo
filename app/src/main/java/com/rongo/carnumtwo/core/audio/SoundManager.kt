package com.rongo.carnumtwo.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.rongo.carnumtwo.R

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val moveSoundId: Int
    private val explodeSoundId: Int
    private val coinSoundId: Int

    private var mediaPlayer: MediaPlayer? = null
    private var isMusicEnabled = true // אפשר לחבר להגדרות בעתיד

    init {
        // הגדרת SoundPool לאפקטים קצרים (יעיל יותר למשחקים)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // עד 5 צלילים במקביל
            .setAudioAttributes(audioAttributes)
            .build()

        // טעינת קבצי הסאונד מה-res/raw
        moveSoundId = soundPool.load(context, R.raw.snd_move, 1)
        explodeSoundId = soundPool.load(context, R.raw.snd_explode, 1)
        coinSoundId = soundPool.load(context, R.raw.snd_coin, 1)

        // הגדרת מוזיקת רקע
        mediaPlayer = MediaPlayer.create(context, R.raw.music_background)
        mediaPlayer?.isLooping = true // לופ אינסופי
        mediaPlayer?.setVolume(0.5f, 0.5f) // עוצמה בינונית כדי לא להפריע לאפקטים
    }

    fun playMove() {
        soundPool.play(moveSoundId, 1f, 1f, 0, 0, 1f)
    }

    fun playExplode() {
        soundPool.play(explodeSoundId, 1f, 1f, 0, 0, 1f)
    }

    fun playCoin() {
        soundPool.play(coinSoundId, 1f, 1f, 0, 0, 1f)
    }

    fun startMusic() {
        if (isMusicEnabled && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun release() {
        soundPool.release()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}