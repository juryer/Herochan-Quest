package com.mathpuzzle.game.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object SoundManager {

    enum class BgmTrack(val rawResId: Int?) {
        MAIN_MENU(null),
        BATTLE(null),
        VICTORY(null),
        GAME_OVER(null)
    }

    enum class SoundEffect {
        SELECT, TEN_COMBO, ATTACK, DAMAGE, BUTTON_CLICK, CLEAR, GAME_OVER
    }

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private var isMuted = false

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(audioAttributes).build()
        loadSounds(context)
    }

    private fun loadSounds(context: Context) {
        // TODO: res/raw に音声ファイルを配置したらコメントを外す
        // soundMap[SoundEffect.SELECT]       = soundPool!!.load(context, R.raw.se_select, 1)
        // soundMap[SoundEffect.DAMAGE]       = soundPool!!.load(context, R.raw.se_damage, 1)
        // soundMap[SoundEffect.BUTTON_CLICK] = soundPool!!.load(context, R.raw.se_button_click, 1)
        // soundMap[SoundEffect.CLEAR]        = soundPool!!.load(context, R.raw.se_clear, 1)
        // soundMap[SoundEffect.GAME_OVER]    = soundPool!!.load(context, R.raw.se_game_over, 1)
    }

    fun playBgm(context: Context, track: BgmTrack, loop: Boolean = true) {
        if (isMuted || track.rawResId == null) return
        stopBgm()
        mediaPlayer = MediaPlayer.create(context, track.rawResId).also {
            it.isLooping = loop
            it.start()
        }
    }

    fun stopBgm() {
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
    }

    fun playSe(effect: SoundEffect) {
        if (isMuted) return
        val soundId = soundMap[effect] ?: return
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun setMuted(muted: Boolean) { isMuted = muted; if (muted) stopBgm() }
    fun isMuted(): Boolean = isMuted

    fun release() {
        stopBgm()
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}
