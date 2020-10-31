package com.bamgasi.voicerecorder.fragment

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.bamgasi.voicerecorder.MyUtils
import com.bamgasi.voicerecorder.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        val recordQulify = findPreference<ListPreference>("key_record_qu")
        val saveLocation = findPreference<ListPreference>("key_save_location")

        val appShare = findPreference<Preference>("app_share")
        val appReview = findPreference<Preference>("app_review")
        val appVersion = findPreference<Preference>("app_version")
        val myApp = findPreference<Preference>("my_app")


        recordQulify?.summary = recordQulify?.entry
        saveLocation?.summary = saveLocation?.entry

        recordQulify?.setOnPreferenceChangeListener { preference, newValue ->
            recordQulify.value = newValue.toString()
            preference?.summary = recordQulify.entry
            true
        }

        saveLocation?.setOnPreferenceChangeListener { preference, newValue ->
            saveLocation.value = newValue.toString()
            preference?.summary = saveLocation.entry
            //AppConfig.setSaveDir()
            true
        }

        appShare?.setOnPreferenceClickListener {
            MyUtils.shareApp()
            true
        }

        appReview?.setOnPreferenceClickListener {
            MyUtils.goToStore()
            true
        }

        myApp?.setOnPreferenceClickListener {
            MyUtils.introMyApp()
            true
        }

        appVersion?.summary = MyUtils.getVersion()
        appVersion?.setOnPreferenceClickListener {
            true
        }
    }
}

