package com.dhiren.atom.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator

class AlarmRinger(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator = appContext.getSystemService(Vibrator::class.java)

    fun start() {
        if (mediaPlayer?.isPlaying == true) return
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(appContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, 700L, 350L, 700L),
                0,
            ),
        )
    }

    fun stop() {
        vibrator.cancel()
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
}
