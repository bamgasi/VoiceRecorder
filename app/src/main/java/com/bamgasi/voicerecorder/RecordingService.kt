package com.bamgasi.voicerecorder

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.IOException

class RecordingService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private val myBinder = MyLocalBinder()

    companion object {
        const val CHANNEL_ID = "com.bamgasi.voicerecorder.CHANNEL_ID"
        const val CHANNEL_NAME = "Coco Recorder"
        const val TAG = "RecordingService"
    }

    private lateinit var mNotification: Notification
    private val mNotificationId: Int = 1000


    override fun onBind(intent: Intent): IBinder {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService() : RecordingService {
            return this@RecordingService
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Log.d(TAG, "Call onStartCommand(): $action")
        when(intent?.getStringExtra("action")) {
            "START" -> {
                val filePath = intent.getStringExtra("filePath")
                var recordQuality = intent.getStringExtra("recordQuality")
                if (recordQuality.isNullOrEmpty()) recordQuality = "NORMAL"

                startRecording(filePath!!, recordQuality)
                startForegroundService()
            }
            "PAUSE" -> {
                pauseRecording()
            }
            "RESUME" -> {
                resumeRecording()
            }
            "STOP" -> {
                stopRecording()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        //Log.d(TAG, "Call onStartCommand()")
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.title_recording_notification))
            .setContentIntent(pendingIntent)

        mNotification = builder.build()

        startForeground(mNotificationId, mNotification)
    }

    private fun startRecording(filePath: String, recordQuality: String) {
        //Log.e(TAG, "startRecording(): $filePath")
        if (mediaRecorder == null) initMediaRecorder(recordQuality)
        mediaRecorder?.setOutputFile(filePath)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initMediaRecorder(recordQuality: String) {
        //Log.e(TAG, "initMediaRecorder(): $recordQuality")
        var asr = 0
        var aebr = 0
        when(recordQuality) {
            "HIGH" -> {
                asr = 48000
                aebr = 256 * 1024
            }
            "NORMAL" -> {
                asr = 44100
                aebr = 128 * 1024
            }
            else -> {
                asr = 22050
                aebr = 64 * 1024
            }
        }

        mediaRecorder = MediaRecorder()
        mediaRecorder?.run {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioSamplingRate(asr)
            setAudioEncodingBitRate(aebr)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pauseRecording() {
        //Log.e(TAG, "pauseRecording()")
        mediaRecorder?.pause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resumeRecording() {
        //Log.e(TAG, "resumeRecording()")
        mediaRecorder?.resume()
    }

    private fun stopRecording() {
        //Log.e(TAG, "stopRecording()")
        mediaRecorder?.run {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
    }

    fun getMaxAmplitude(): Int {
        var maxValue = mediaRecorder?.maxAmplitude
        if (maxValue == null) maxValue = 0
        /**
         * 그래프가 너무 작게 보여서 값을 2배로 증가시켰다.
         */
        return maxValue
    }

    override fun onDestroy() {
        //Log.e(TAG, "onDestroy()")
        stopRecording()
        stopSelf()
        super.onDestroy()
    }
}