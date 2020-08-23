package com.bamgasi.voicerecorder

import android.app.Application

class MyApplication: Application() {
    companion object {
        var instance: MyApplication? = null

        fun getGlobalApplicationContext() : MyApplication? {
            checkNotNull(this) { "this application does not inherit MyApplication" }
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
    }
}