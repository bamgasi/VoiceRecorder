package com.bamgasi.voicerecorder.model

import android.net.Uri

data class Records(
    var id: Long,
    var recordName: String,
    var recordTime: String,
    var recordDate: String,
    var recordUri: Uri,
    var fileSize: String
)