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
    private val lockedSoundId: Int // New ID

    private var mediaPlayer: MediaPlayer? = null

    // Volume levels (Internal float used for playback)
    private var musicVolume: Float = 0.125f
    private var sfxVolume: Float = 0.25f

    private val MUSIC_MAX_VOLUME_SCALE = 0.25f
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

        // Load Locked Sound
        lockedSoundId = soundPool.load(context, R.raw.snd_locked, 1)

        mediaPlayer = MediaPlayer.create(context, R.raw.music_background)
        mediaPlayer?.isLooping = true
    }

    fun setVolumes(musicVolInt: Int, sfxVolInt: Int) {
        sfxVolume = (sfxVolInt / 100f) * SFX_MAX_VOLUME_SCALE
        musicVolume = (musicVolInt / 100f) * MUSIC_MAX_VOLUME_SCALE

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

    // New function for locked sound
    fun playLocked() {
        soundPool.play(lockedSoundId, sfxVolume, sfxVolume, 0, 0, 1f)
    }

    fun startMusic() {
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