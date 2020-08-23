package com.bamgasi.voicerecorder

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File


class AppConfig {
    companion object {
        private lateinit var saveDirString: String
        private lateinit var saveLocation: String
        private lateinit var recordQuality: String
        private val TAG = "AppConfig"
        val FILE_EXT = ".mp3"

        fun getVersionInfo(): String {
            val context = MyApplication.instance as Context
            val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = info.versionName

            return version
        }

        fun setSaveDir() {
            saveDirString = Environment.getExternalStorageDirectory().absolutePath + "/kokorecorder/"

            val saveDirectory = File(saveDirString)
            if (!saveDirectory.exists()) {
                val mkResult = saveDirectory.mkdirs()
                Log.e(TAG, "mkdirs: ${saveDirString}: ${mkResult}")
            }
        }

        fun getSaveDir(): String {
            return saveDirString
        }

        fun getRecordQuality(): String {
            val shared = PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
            val record_qu = shared.getString("key_record_qu", "")
            if (record_qu != null) {
                recordQuality = record_qu
            }else{
                recordQuality = "NORMAL"
            }

            return recordQuality
        }

        fun getSaveLocation(): String {
            val shared = PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
            val record_qu = shared.getString("key_save_location", "")
            if (record_qu != null) {
                saveLocation = record_qu
            }else{
                saveLocation = "INTERNAL"
            }

            return saveLocation
        }

        fun getExternalStoragePath(): String? {
            val internalPath =
                Environment.getExternalStorageDirectory().absolutePath
            val paths = internalPath.split("/".toRegex()).toTypedArray()
            var parentPath = "/"
            for (s in paths) {
                if (s.trim { it <= ' ' }.length > 0) {
                    parentPath = parentPath + s
                    break
                }
            }
            val parent = File(parentPath)
            if (parent.exists()) {
                val files = parent.listFiles()
                for (file in files) {
                    val filePath = file.absolutePath
                    Log.d(TAG, filePath)
                    if (filePath == internalPath) {
                        continue
                    } else if (filePath.toLowerCase().contains("sdcard")) {
                        return filePath
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        try {
                            if (Environment.isExternalStorageRemovable(file)) {
                                return filePath
                            }
                        } catch (e: RuntimeException) {
                            Log.e(TAG, "RuntimeException: $e")
                        }
                    }
                }
            }
            return null
        }
    }
}
