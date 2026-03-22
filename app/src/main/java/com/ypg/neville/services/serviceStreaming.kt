package com.ypg.neville.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.myReceiver
import com.ypg.neville.model.utils.utilsFields
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class serviceStreaming : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var exec: ExecutorService? = null

    override fun onCreate() {
        mserviseThis = this
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Enviando un mensaje al receiver
        val intent11 = Intent()
        intent11.action = "com.ypg.neville.action.streaming.signal"
        intent11.putExtra("action", "Yorjandis")
        applicationContext.sendBroadcast(intent11)

        if (intent?.action?.contains("play_medio") == true) {
            try {
                if (exec?.isTerminated == false) {
                    exec?.shutdownNow()
                }
            } catch (ignored: Exception) {
            }

            exec = Executors.newSingleThreadExecutor()
            exec?.execute {
                mediaPlayer = MediaPlayer.create(applicationContext, Uri.parse(intent.getStringExtra("file")))
                
                mediaPlayer?.let {
                    createNotificacion(intent.getStringExtra("title") ?: "", it.duration.toLong() / 60000)
                    
                    it.isLooping = PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("play_video_playloop", false)
                    it.start()
                    
                    it.setOnCompletionListener { }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        exec?.shutdownNow()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    private fun createNotificationChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(CHANEL_ID, "Foreground Notification", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createNotificacion(titulo: String, duraMinutos: Long) {
        createNotificationChanel()
        val intent_openActivity = Intent(this, MainActivity::class.java)
        intent_openActivity.action = "yor"
        val pendingIntent_open = PendingIntent.getActivity(this, 5, intent_openActivity, PendingIntent.FLAG_IMMUTABLE)

        val intent_Stop = Intent(this, myReceiver::class.java)
        intent_Stop.action = myReceiver.ACTION_SIGNAL
        intent_Stop.putExtra("action", "stop")
        val pendingIntent_stop = PendingIntent.getBroadcast(this, 0, intent_Stop, PendingIntent.FLAG_IMMUTABLE)

        val intent_pause = Intent(this, myReceiver::class.java)
        intent_pause.action = myReceiver.ACTION_SIGNAL
        intent_pause.putExtra("action", "pause")
        val pendingIntent_pause = PendingIntent.getBroadcast(this, 1, intent_pause, PendingIntent.FLAG_IMMUTABLE)

        val intent_resume = Intent(this, myReceiver::class.java)
        intent_resume.action = myReceiver.ACTION_SIGNAL
        intent_resume.putExtra("action", "resume")
        val pendingIntent_resume = PendingIntent.getBroadcast(this, 2, intent_resume, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANEL_ID)
            .setContentTitle("Neville - play ($duraMinutos minutos)")
            .setContentText(titulo)
            .setSmallIcon(R.drawable.ic_item)
            .setContentIntent(pendingIntent_open)
            .addAction(R.drawable.ic_item, "Detener", pendingIntent_stop)
            .addAction(R.drawable.ic_item, "Pausar", pendingIntent_pause)
            .addAction(R.drawable.ic_item, "Resumir", pendingIntent_resume)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun serviceStop() {
        try {
            mediaPlayer?.release()
            exec?.shutdownNow()
        } catch (ignored: Exception) {
        }
        stopSelf()
    }

    fun stopMediaP() {
        try {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
        } catch (ignored: Exception) {
        }
    }

    fun pauseMediaP() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun startMediaP() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.start()
        }
    }

    fun SetDataSourceMediaP(source: String) {
        try {
            mediaPlayer?.setDataSource(source)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun releaseMediaP() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (ignored: Exception) {
        }
    }

    fun loopingMediaP(flag: Boolean) {
        mediaPlayer?.isLooping = flag
    }

    fun getDurationMediaP(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun playMediaP(DirRepo: String, file: String, plooping: Boolean) {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.reset()

            val source = File.separator + "sdcard" + File.separator + utilsFields.REPO_DIR_ROOT + File.separator + DirRepo + File.separator + file
            try {
                it.setDataSource(source)
                try {
                    it.prepare()
                    it.isLooping = plooping
                    it.start()
                } catch (ignored: Exception) {
                }
            } catch (ignored: Exception) {
            }
        }
    }

    companion object {
        @JvmField
        var mserviseThis: serviceStreaming? = null
        const val CHANEL_ID = "neville1"
        const val NOTIFICATION_ID = 1221

        @JvmStatic
        fun updateNotification(context: Context, titulo: String, duraMinutos: Long) {
            val intent_openActivity = Intent(context, MainActivity::class.java)
            intent_openActivity.action = "yor"
            val pendingIntent_open = PendingIntent.getActivity(context, 5, intent_openActivity, PendingIntent.FLAG_IMMUTABLE)

            val intent_Stop = Intent(context, myReceiver::class.java)
            intent_Stop.action = myReceiver.ACTION_SIGNAL
            intent_Stop.putExtra("action", "stop")
            val pendingIntent_stop = PendingIntent.getBroadcast(context, 0, intent_Stop, PendingIntent.FLAG_IMMUTABLE)

            val intent_pause = Intent(context, myReceiver::class.java)
            intent_pause.action = myReceiver.ACTION_SIGNAL
            intent_pause.putExtra("action", "pause")
            val pendingIntent_pause = PendingIntent.getBroadcast(context, 1, intent_pause, PendingIntent.FLAG_IMMUTABLE)

            val intent_resume = Intent(context, myReceiver::class.java)
            intent_resume.action = myReceiver.ACTION_SIGNAL
            intent_resume.putExtra("action", "resume")
            val pendingIntent_resume = PendingIntent.getBroadcast(context, 2, intent_resume, PendingIntent.FLAG_IMMUTABLE)

            val notificationupdate = NotificationCompat.Builder(context, CHANEL_ID)
                .setContentTitle("Neville - play ($duraMinutos minutos)")
                .setContentText(titulo)
                .setSmallIcon(R.drawable.ic_item)
                .setContentIntent(pendingIntent_open)
                .addAction(R.drawable.ic_item, "Detener", pendingIntent_stop)
                .addAction(R.drawable.ic_item, "Pausar", pendingIntent_pause)
                .addAction(R.drawable.ic_item, "Resumir", pendingIntent_resume)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notificationupdate)
        }
    }
}
