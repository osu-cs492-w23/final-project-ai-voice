package com.example.myapplication.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.myapplication.R
import com.example.myapplication.voicedatabase.VoiceDBViewModel
import com.example.myapplication.voicedatabase.VoiceDao
import com.example.myapplication.voicedatabase.VoiceDatabaseItem

class SettingsFragment: PreferenceFragmentCompat() {

    private lateinit var voiceDBViewModel: VoiceDBViewModel
    private lateinit var globalPreferredVoice: String




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val key = getString(R.string.pref_voice_key)
        val listPref = findPreference<ListPreference>(getString(R.string.pref_voice_key))


        voiceDBViewModel = ViewModelProvider(this).get(VoiceDBViewModel::class.java)


        val preference = findPreference<Preference>(key)
        val value = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString(preference?.key, "")


            voiceDBViewModel.searchVoice(value!!).observe(viewLifecycleOwner){it ->
                if(it != null){
                    Log.d("ttttt", it.name)

                    preference?.summaryProvider = Preference.SummaryProvider<Preference> { preference ->
                        val value = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString(preference.key, "")
                        "Selected value: ${it.name}"
                    }

                    globalPreferredVoice = it.name

                }
            }






    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        voiceDBViewModel = ViewModelProvider(this).get(VoiceDBViewModel::class.java)

        val voiceNames: MutableList<String> = mutableListOf()
        val voiceIDs: MutableList<String> = mutableListOf()


        val listPref = findPreference<ListPreference>(getString(R.string.pref_voice_key))


        voiceDBViewModel = ViewModelProvider(this).get(VoiceDBViewModel::class.java)

        voiceDBViewModel.allVoices.observe(this, Observer { list ->

            for(voice in list){
                voiceNames.add(voice.name)
                voiceIDs.add(voice.voice_id)
            }

            val entries = voiceNames
            val entryValues = voiceIDs
//
            listPref?.apply {
                setEntries(entries.toTypedArray())
                setEntryValues(entryValues.toTypedArray())
            }

        })

        try{
            listPref?.setSummary("1")
        }catch(e: Exception){
            Log.d("exception", e.toString())
        }


        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

//
//
        listPref?.setOnPreferenceChangeListener { preference, newValue ->
            val keys = getString(R.string.pref_voice_key)
            sharedPreferences?.edit()?.putString(keys, newValue.toString())?.apply()
            Log.d("newvalue", newValue.toString())

            val value = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(preference.key, "")

            var newlySelectedVoice: String? = null

            voiceDBViewModel.searchVoice(value!!).observe(viewLifecycleOwner){it ->

                newlySelectedVoice = it?.name
            }


            ///Whoooooa I finally figured it out.
            preference?.summaryProvider = Preference.SummaryProvider<Preference> { preference ->

                Log.d("globalPreferredVoice", globalPreferredVoice)

                "Selected value: ${newlySelectedVoice}"
            }
            true
        }

    }


    //Src = https://developer.android.com/develop/ui/views/components/settings/customize-your-settings



    override fun onPause() {
        super.onPause()

        val preferenceScreen = preferenceScreen
        if(preferenceScreen != null ){
            val sharedPrefs = preferenceScreen.sharedPreferences
            sharedPrefs?.edit()?.apply()
        }

        //Saves preference when user navigates away
        //Src: In java, but it helped: https://stackoverflow.com/questions/24039577/android-sharedpreferences-editing-not-working
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().apply()
    }


    }

