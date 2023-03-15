package com.example.myapplication.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.HistoryAdapter
import com.example.myapplication.data.HistoryResponse
import com.example.myapplication.data.Voice
import com.example.myapplication.data.VoiceAdapter
import com.example.myapplication.ui.*

class HistoryViewFragment: Fragment(R.layout.history_view) {

    private lateinit var arrayAdapter: HistoryAdapter

    private lateinit var voiceSpinner: Spinner

    private lateinit var voiceNames: List<String>

    private lateinit var spinner: Spinner

    private lateinit var spinnerAdapter: HistoryAdapter

    private val voiceArray = mutableListOf<Voice>()


    private val voiceViewModel: ListOfVoicesViewModel by viewModels()

    private val historySearchViewModel: HistorySearchViewModel by viewModels()

    private lateinit var voiceAdapter: VoiceAdapter

    private var historyItems: HistoryResponse? = null

    private val TAG = "HistoryViewActivity"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        spinner = view.findViewById<Spinner>(R.id.spinner_history)


        val button: Button = view.findViewById(R.id.button_history)

        if (historySearchViewModel.historySearchResults.value == null) {
            historySearchViewModel.loadHistorySearchResults()
        }

        historySearchViewModel.historySearchResults.observe(viewLifecycleOwner) { histResults ->
            if (histResults != null) {
                historyItems = histResults

                for (hist in historyItems!!.history) {
                    hist.url = uriSchemeBuilder(hist.history_item_id)
                    Log.d("histurl", hist.url.toString())
                }

            }
        }

        Log.d("history", historyItems.toString())


        if (voiceViewModel.voiceListResults.value == null) {

            voiceViewModel.loadListOfVoices()

        }

        voiceViewModel.voiceListResults.observe(viewLifecycleOwner) { results ->

            if (results != null) {
                for (voice in results.voices) {
                    voiceArray.add(voice)
                    Log.d("NewVoiceAdded", voice.toString())
                }
                spinner.adapter = HistoryAdapter(requireContext(), voiceArray)
            }
        }

        voiceNames = voiceArray.map { it.name }


        button.setOnClickListener {

            val directions = HistoryViewFragmentDirections.navigateToHistoryBySelectedVoice(
                spinner.selectedItem as Voice, historyItems as HistoryResponse
            )
            findNavController().navigate(directions)

        }



        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedVoice = p0?.getItemAtPosition(p2) as? Voice

                selectedVoice?.let {
                    Toast.makeText(
                        requireContext(),
                        "Selected item: ${it.voice_id}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Log.d("selection", selectedVoice!!.name)

            }


        }
    }

    override fun onStart() {
        super.onStart()


    }

    private fun uriSchemeBuilder(audioId: String): Uri {
        return Uri.parse("https://api.elevenlabs.io/v1/history/${audioId}/audio")
    }

}






