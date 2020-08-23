package com.bamgasi.voicerecorder.fragment

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.bamgasi.voicerecorder.AppConfig
import com.bamgasi.voicerecorder.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        val appVersion = findPreference<PreferenceScreen>("key_app_version")
        val recordQulify = findPreference<ListPreference>("key_record_qu")
        val saveLocation = findPreference<ListPreference>("key_save_location")

        appVersion?.summary = AppConfig.getVersionInfo()

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
            AppConfig.setSaveDir()
            true
        }
    }


}

