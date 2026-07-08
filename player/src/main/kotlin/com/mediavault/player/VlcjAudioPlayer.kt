package com.mediavault.player

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer

class VlcjAudioPlayer {
    private val factory = MediaPlayerFactory()
    private val mediaPlayer: MediaPlayer = factory.mediaPlayers().newMediaPlayer()

    fun play(path: String) {
        mediaPlayer.media().play(path)
    }

    fun pause() {
        mediaPlayer.controls().pause()
    }

    fun resume() {
        mediaPlayer.controls().play()
    }

    fun seek(positionMillis: Long) {
        mediaPlayer.controls().setTime(positionMillis)
    }

    fun stop() {
        mediaPlayer.controls().stop()
    }

    fun release() {
        mediaPlayer.release()
        factory.release()
    }
}
