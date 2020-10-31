package com.bamgasi.voicerecorder.fragment

import android.app.AlertDialog
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bamgasi.voicerecorder.AppConfig
import com.bamgasi.voicerecorder.R
import com.bamgasi.voicerecorder.RecordingService
import com.bamgasi.voicerecorder.model.RecordingFile
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_record.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timer

class RecordFragment : Fragment() {
    private var time = 0
    private var timerTask: Timer? = null
    private val TAG = "RecordFragment"
    private var recordingStopped: Boolean = false
    private var state: Boolean = false
    private lateinit var recordingFile: RecordingFile
    private var FINISH_INTERVAL_TIME: Long = 2000
    private var backPressedTime: Long = 0
    private val serviceClass = RecordingService::class.java

    var recordingService: RecordingService? = null
    var isService = false
    private var mConnection: ServiceConnection? = null

    private fun bindService() {
        mConnection = object: ServiceConnection{
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as RecordingService.MyLocalBinder
                recordingService = binder.getService()
                isService = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isService = false
            }
        }

        val intent = Intent(requireActivity(), serviceClass)
        requireActivity().bindService(intent, mConnection as ServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    val tempTime = System.currentTimeMillis();
                    val intervalTime = tempTime - backPressedTime;
                    if (intervalTime in 0..FINISH_INTERVAL_TIME) {
                        activity?.finish()
                    } else {
                        backPressedTime = tempTime;
                        Toast.makeText(context, R.string.msg_finish_alert, Toast.LENGTH_SHORT).show();
                        return
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        MobileAds.initialize(context) { }

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_record.setOnClickListener {
            if (!state) {
                startRecording()
            }else{
                popSaveAlert()
            }
            setButton()
        }

        btn_pause.setOnClickListener {
            pauseRecording()
            setButton()
        }

        btn_cancel.setOnClickListener {
            popCancelAlert()
        }

        setButton()
    }

    private fun popCancelAlert() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.title_btn_cancel)
            .setMessage(R.string.message_recording_cancel)
            .setPositiveButton(R.string.title_btn_ok) { p0, p1 ->
                cancelRecording()

                // 캐시 파일 삭제
                val cacheFile = File(recordingFile.filePath)
                cacheFile.delete()
                setButton()
                showNavView(true)
            }
            .setNegativeButton(R.string.title_btn_cancel) { p0, p1 ->

            }
        builder.create()
        builder.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    private fun setButton() {
        if (state) {
            btn_pause.visibility = View.VISIBLE
            btn_cancel.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(context, R.anim.blink_animation);
            if (recordingStopped) {
                btn_pause.apply {
                    startAnimation(anim)
                }
                tv_recording_state.apply {
                    setText(R.string.title_pause)
                    startAnimation(anim)
                }
            }else{
                btn_pause.clearAnimation()

                tv_recording_state.apply {
                    setText(R.string.title_recording)
                    clearAnimation()
                }
            }

            btn_record.setImageResource(R.drawable.stop)
        } else {
            tv_recording_state.text = ""
            btn_pause.clearAnimation()
            btn_pause.visibility = View.GONE
            btn_cancel.visibility = View.GONE
            btn_record.setImageResource(R.drawable.record)
        }
    }

    private fun setRecordingFilename() {
        recordingFile = RecordingFile()
        recordingFile.fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        recordingFile.fileExt = ".mp3"
        recordingFile.filePath = context?.externalCacheDir?.absolutePath+"/"+recordingFile.fileName+recordingFile.fileExt
    }

    private fun startRecording() {
        var recordQuality = AppConfig.getRecordQuality()

        try {
            setRecordingFilename()
            if (!isService) bindService()
            startService("START", recordingFile.filePath!!, recordQuality)
            state = true
            startTimer()
            showNavView(false)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun showNavView(show: Boolean) {
        val navView: BottomNavigationView? = activity?.findViewById(R.id.nav_view)
        if (navView != null) {
            if (show) {
                navView.visibility = View.VISIBLE
                val animate = TranslateAnimation(0F, 0F, navView.height.toFloat(), 0F)
                animate.duration = 500
                animate.fillAfter = true
                navView.startAnimation(animate)
            }else{
                navView.visibility = View.GONE
                val animate = TranslateAnimation(0F, 0F, 0F, navView.height.toFloat())
                animate.duration = 500
                animate.fillAfter = true
                navView.startAnimation(animate)
            }
        }
    }

    private fun stopRecording() {
        //Log.e(TAG, "stopRecording()")
        if (state) {
            state = false
            recordingStopped = false
            audioRecordView.recreate()
            stopTimer()

            startService("STOP")

            Toast.makeText(context, R.string.msg_save_complete, Toast.LENGTH_SHORT).show()
            setButton()

        }else{
            Toast.makeText(context, R.string.msg_not_save_state, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelRecording() {
        if (state) {
            state = false
            recordingStopped = false

            startService("STOP")
            stopService()

            audioRecordView.recreate()
            stopTimer()
        }else{
            Toast.makeText(context, R.string.msg_not_save_state, Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseRecording() {
        //Log.e(TAG, "pauseRecording()")
        if (state) {
            if (!recordingStopped) {
                startService("PAUSE")
                recordingStopped = true
                timerTask?.cancel()
            } else {
                resumeRecording()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resumeRecording() {
        //Log.e(TAG, "resumeRecording()")
        startService("RESUME")
        recordingStopped = false
        startTimer()
    }

    private fun startTimer() {
        //Log.e(TAG, "startTimer()")
        timerTask = timer(period = 10) {
            time++
            val min = time / 100 / 60
            val sec = (time / 100) % 60
            val mili = time % 100

            val printTimer = String.format("%02d:%02d.%02d", min, sec, mili)

            lifecycleScope.launch {
                if (tv_recoding_timer != null) tv_recoding_timer.text = printTimer
                //if (audioRecordView != null && mediaRecorder != null) audioRecordView.update(mediaRecorder!!.maxAmplitude)
                if (audioRecordView != null) {
                    val amplitude = recordingService?.getMaxAmplitude()
                    if (amplitude != null) {
                        audioRecordView.update(amplitude)
                    }
                }
            }
        }
    }

    private fun stopTimer() {
        //Log.e(TAG, "stopTimer()")
        timerTask?.cancel()
        tv_recoding_timer.postDelayed(Runnable {
            time = 0
            tv_recoding_timer.text = "00:00.00"
        }, 100)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun popSaveAlert() {
        val builder = AlertDialog.Builder(activity)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null)
        val saveName = view.findViewById<EditText>(R.id.save_name)

        val manager: InputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        saveName.setText(recordingFile.fileName)
        saveName.postDelayed({
            kotlin.run {
                saveName.setSelectAllOnFocus(true)
                saveName.requestFocus()
                manager.showSoftInput(saveName, 0)
            }
        }, 100)

        recordingStopped = false
        pauseRecording()

        builder.setView(view)
            .setTitle(R.string.title_file_save)
            .setPositiveButton(R.string.title_btn_save) { p0, p1 ->
                val fileName = saveName.text.toString()+recordingFile.fileExt
                stopRecording()
                GlobalScope.launch(Dispatchers.Main) {
                    saveRecordingFile(fileName, recordingFile.filePath)
                }

                showNavView(true)
                stopService()
            }
            .setNegativeButton(R.string.title_btn_cancel) { p0, p1 -> }
        val dialog = builder.create()
        dialog.show()

        saveName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
            }
        })
    }

    /*override fun onDestroyView() {
        super.onDestroyView()
        if (state) {
            state = false
            recordingStopped = false
            Toast.makeText(context, R.string.msg_record_stop, Toast.LENGTH_SHORT).show()
            mediaRecorder?.run {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
        }
    }*/

    private suspend fun saveRecordingFile(fileName: String?, absoluteFile: String?) {
        val myFolder = "/KokoRecorder"
        val mimeType = "audio/*"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fileContents = Files.readAllBytes(Paths.get(absoluteFile))
            val values = ContentValues()
            with(values) {
                put(MediaStore.Files.FileColumns.TITLE, fileName)
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + myFolder)
                put(MediaStore.Files.FileColumns.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }

            withContext(Dispatchers.IO) {
                val uri = context?.contentResolver?.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                val fileOutputStream = context?.contentResolver?.openOutputStream(uri!!)
                fileOutputStream?.run {
                    write(fileContents)
                    close()
                }
                values.clear()
                values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                context?.contentResolver?.update(uri!!, values, null, null)

                // 캐시 파일 삭제
                val cacheFile = File(absoluteFile!!)
                cacheFile.delete()
            }

        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() +
                    File.separator + myFolder
            val file = File(dir)
            if (!file.exists()) {
                file.mkdirs()
            }

            withContext(Dispatchers.IO) {
                val targetFile = File(file, fileName!!)
                File(absoluteFile!!).copyTo(targetFile, true, DEFAULT_BUFFER_SIZE)

                val values = ContentValues()
                with(values) {
                    put(MediaStore.Audio.Media.TITLE, fileName)
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.DATA, targetFile.absolutePath)
                }

                context?.contentResolver?.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

                // 캐시 파일 삭제
                val cacheFile = File(absoluteFile!!)
                cacheFile.delete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    private fun stopService() {
        if (isService) {
            mConnection?.let { requireActivity().unbindService(it) }
            isService = false
        }

        val serviceIntent = Intent(requireContext(), serviceClass)
        requireActivity().stopService(serviceIntent)
    }

    private fun startService(action: String, filePath: String = "", recordQuality: String = "") {
        val intent = Intent(requireActivity(), serviceClass)
        intent.putExtra("action", action)
        if (action == "START") {
            intent.putExtra("filePath", filePath)
            intent.putExtra("recordQuality", recordQuality)
        }
        requireActivity().startService(intent)
    }
}