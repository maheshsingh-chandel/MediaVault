package com.mediavault.player

import java.awt.BorderLayout
import javax.swing.JPanel
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

class VlcjVideoPlayerPanel : JPanel(BorderLayout()) {
    private val component = EmbeddedMediaPlayerComponent()

    init {
        add(component, BorderLayout.CENTER)
    }

    fun play(path: String) {
        component.mediaPlayer().media().play(path)
    }

    fun pause() {
        component.mediaPlayer().controls().pause()
    }

    fun resume() {
        component.mediaPlayer().controls().play()
    }

    fun seek(positionMillis: Long) {
        component.mediaPlayer().controls().setTime(positionMillis)
    }

    fun toggleFullscreen() {
        component.mediaPlayer().fullScreen().toggle()
    }

    fun release() {
        component.release()
    }
}
