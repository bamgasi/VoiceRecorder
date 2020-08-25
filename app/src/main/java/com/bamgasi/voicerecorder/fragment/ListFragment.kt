package com.bamgasi.voicerecorder.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bamgasi.voicerecorder.R
import com.bamgasi.voicerecorder.RecordAdapter
import com.bamgasi.voicerecorder.model.Records
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.custom_music_player.*
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ListFragment : Fragment() {
    private val TAG = "ListFragment"
    private var player: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private var currentWindow = 0
    private var playWhenReady = true
    private val mediaReceiver = MediaReceiver()
    private var curPlayUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_list, container, false)
        return root
    }

    suspend fun getRecordList() = withContext(Dispatchers.IO) {
        var recordList = arrayListOf<Records>()
        try {
            recordList = getFileList(requireContext()) as ArrayList<Records>
        }catch (e: Exception) {
            //Toast.makeText(context, "녹음목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        recordList
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch(Main) {
            /**
             * 로딩 화면은 일단 빼자. 나중에 문제가 된다면 넣는 방법을 모색
             * 1초 이내에 리스트 가져오기가 안되면 1초 이후 로딩 등등.
             */
            /*val loadingDialog = context?.let { LoadingDialog(it) }
            loadingDialog?.show()*/

            val recordList: ArrayList<Records> = getRecordList()
            record_recycler.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
                adapter = RecordAdapter(recordList, this@ListFragment, context) { record ->
                    curPlayUri = record.recordUri
                    startPlaying(curPlayUri)
                }
            }
            /*loadingDialog?.dismiss()*/
        }

        initializePlayer()

        player?.addListener(object: Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    /*player?.seekTo(0)
                    player?.playWhenReady = false*/
                }
            }
        })

        btn_hide_player.setOnClickListener {
            main_pcv.visibility = View.GONE
            stopPlayer()
        }
    }

    fun getFileName(orgName: String): String {
        val pos = orgName.lastIndexOf(".")
        val retValue = if (pos < 0) {
            orgName
        }else{
            orgName.substring(0, pos)
        }
        return retValue
    }

    private fun startPlaying(uri: Uri?) {
        if (player != null) {
            //val uri = Uri.parse(filePath)
            val mediaSource = buildMediaSource(uri!!)

            mediaSource?.let { player!!.prepare(it) }
            /*player!!.seekTo(currentWindow, playbackPosition)
            player!!.playWhenReady = playWhenReady*/
            player!!.seekTo(0)
            player!!.playWhenReady = true
            main_pcv.visibility = View.VISIBLE
        }
    }

    fun stopPlayer() {
        if (player != null) {
            if (player!!.isPlaying) player!!.setPlayWhenReady(false)
        }
    }

    fun resetPlayer() {
        if (player != null) {
            player?.seekTo(0)
            player?.playWhenReady = false
            main_pcv.visibility = View.GONE
        }
    }

    fun initializePlayer() {
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(requireContext())

            main_pcv.player = player
            main_pcv.showTimeoutMs = 0
            setAudioFocus()
            main_pcv.visibility = View.GONE
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource? {
        return ExtractorMediaSource.Factory(
            DefaultDataSourceFactory(activity, getString(R.string.app_name))
        ).createMediaSource(uri)
    }

    fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            playWhenReady = it.playWhenReady
            it.release()
            player = null
        }
    }

    override fun onResume() {
        //Log.e(TAG, "Call onResume()")
        super.onResume()
        initializePlayer()
        context?.registerReceiver(
            mediaReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
        context?.unregisterReceiver(mediaReceiver)
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun setAudioFocus() {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val afChangeListener: AudioManager.OnAudioFocusChangeListener =
            AudioManager.OnAudioFocusChangeListener {
                when (it) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        player?.playWhenReady = false
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        player?.playWhenReady = false
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        player?.playWhenReady = true

                    }
                }
            }
        val result: Int = audioManager.requestAudioFocus(
            afChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        player?.playWhenReady = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    inner class MediaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                player?.playWhenReady = false
            }
        }
    }

    fun getFileList(context: Context): List<Records> {
        val myFolder = arrayOf("%KokoRecorder%")

        /**
         * DATE_TAKEN 컬럼으로 시도하니 값이 잘 나오지 않는다.
         * 그래서 추가일을 기준으로 데이터를 가져왔다.
         */
        val fileList = arrayListOf<Records>()

        /**
         * MediaStore.Files.FileColumns.DURATION 상수가 값자기
         * API 29 부터 지원한다고 해서, 어쩔수 없이 Q 이하는 DATA를 참조하여
         * Full Path를 활용하여 Duration을 구한다.
         */
        val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.DATA)

        /**
         * MediaStore.Audio.Media.RELATIVE_PATH 가 Q 부터 사용가능하여
         * 그 이하는 FullPath를 저장하고 있는 DATA 값을 활용한다.
         */
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        }else{
            MediaStore.Audio.Media.DATA
        }

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "$selection like ? ",
            myFolder,
            sortOrder
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = SimpleDateFormat("yyyy.MM.dd HH:mm").format(
                    cursor.getLong(
                        dateTakenColumn
                    ) * 1000L
                )

                val contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                val displayName = cursor.getString(displayNameColumn)
                val dataName = cursor.getString(dataColumn)
                var durationLong = cursor.getLong(durationColumn)
                if (durationLong == 0L) {
                    durationLong = getDuration(dataName)
                }

                val duration = convertDuration(durationLong)

                /*Log.d(
                    "test",
                    "id: $id, display_name: $displayName, date_taken: $dateTaken, duration: $durationLong, content_uri: $contentUri\n"
                )*/

                fileList.add(Records(id, getFileName(displayName), duration, dateTaken, contentUri))
            }
        }

        cursor?.close()

        return fileList
    }

    fun getDuration(filePath: String): Long {
        //Log.e(TAG, "filePath: $filePath")
        val mediaMetadataRetriever = MediaMetadataRetriever()
        var milisec = 0L

        try {
            mediaMetadataRetriever.setDataSource(filePath);
            val time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            milisec = java.lang.Long.parseLong(time)
        }catch (e: java.lang.Exception) {
            e.printStackTrace()
            milisec = 0L
        }

        return milisec
    }

    fun convertDuration(duration: Long): String {
        //Log.e(TAG, "duration: $duration")
        val hour = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration))
        val second = TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))

        return if (hour > 0) {
            String.format("%02d:%02d:%02d", hour, minutes, second)
        }else{
            String.format("%02d:%02d", minutes, second)
        }
    }
}