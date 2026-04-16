package com.ypg.neville.feature.voice.media

import android.media.MediaPlayer

class AndroidVoicePlayerEngine : VoicePlayerEngine {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPath: String? = null

    override fun play(filePath: String, onCompletion: () -> Unit) {
        stop()

        val player = MediaPlayer().apply {
            setDataSource(filePath)
            setOnCompletionListener {
                stop()
                onCompletion()
            }
            prepare()
            start()
        }

        mediaPlayer = player
        currentPath = filePath
    }

    override fun stop() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            player.reset()
            player.release()
        }
        mediaPlayer = null
        currentPath = null
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    override fun currentFilePath(): String? = currentPath

    override fun release() {
        stop()
    }
}
