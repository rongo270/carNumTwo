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

    // Volume levels (Internal float used for playback)
    private var musicVolume: Float = 0.125f // Default start approx 12.5% (50 slider * 0.25 scale)
    private var sfxVolume: Float = 0.25f    // Default start 25% (50 slider * 0.5 scale)

    // --- Volume Scaling Constants ---
    // Even if slider is at 100%, Music will be 25% real volume
    private val MUSIC_MAX_VOLUME_SCALE = 0.25f
    // Even if slider is at 100%, SFX will be 50% real volume
    private val SFX_MAX_VOLUME_SCALE = 0.50f

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        moveSoundId = soundPool.load(context, R.raw.snd_move, 1)
        explodeSoundId = soundPool.load(context, R.raw.snd_explode, 1)
        coinSoundId = soundPool.load(context, R.raw.snd_coin, 1)

        mediaPlayer = MediaPlayer.create(context, R.raw.music_background)
        mediaPlayer?.isLooping = true
    }

    // Call this from Activity to update volumes dynamically
    fun setVolumes(musicVolInt: Int, sfxVolInt: Int) {
        // SFX: 0-100 -> Scaled down to max 50%
        // Example: Slider 100 -> 1.0 * 0.5 = 0.5 (50% real volume)
        // Example: Slider 50  -> 0.5 * 0.5 = 0.25 (25% real volume)
        sfxVolume = (sfxVolInt / 100f) * SFX_MAX_VOLUME_SCALE

        // MUSIC: 0-100 -> Scaled down to max 25%
        musicVolume = (musicVolInt / 100f) * MUSIC_MAX_VOLUME_SCALE

        // Update playing music immediately
        if (mediaPlayer != null) {
            mediaPlayer?.setVolume(musicVolume, musicVolume)
        }
    }

    fun playMove() {
        soundPool.play(moveSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
    }

    fun playExplode() {
        soundPool.play(explodeSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
    }

    fun playCoin() {
        soundPool.play(coinSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
    }

    fun startMusic() {
        // Check if volume > 0 before starting
        if (musicVolume > 0 && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.setVolume(musicVolume, musicVolume)
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