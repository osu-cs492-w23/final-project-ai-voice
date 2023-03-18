package com.example.myapplication.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.data.HistoryItem
import com.example.myapplication.data.HistoryResponse
import com.example.myapplication.data.Voice
import com.example.myapplication.database.HistoryDBViewModel
import com.example.myapplication.database.HistoryDatabaseItem
import com.example.myapplication.ui.HistorySearchViewModel
import com.example.myapplication.ui.MediaViewModel
import com.example.myapplication.ui.VoiceInterface
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.io.path.pathString
import kotlin.io.path.writeBytes

class QuickGenerateFragment: Fragment(R.layout.quick_generate) {


    private lateinit var mediaViewModel: MediaViewModel

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val voiceservice = VoiceInterface.create()

    private var newVoice: Voice? = null

    private var filePath: String? = null

    private lateinit var loadingIndicator: CircularProgressIndicator

    private val args: VoiceGeneratorFragmentArgs by navArgs()

    private var currentlyGenerating: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val historySearchViewModel:  HistorySearchViewModel by viewModels()

        val navView: NavigationView = requireActivity().findViewById(R.id.nav_view)

        val navmenu = navView.menu



        val historyVM: HistoryDBViewModel by viewModels()

        mediaViewModel = ViewModelProvider(requireActivity()).get(MediaViewModel::class.java)


        historySearchViewModel.loadHistorySearchResults()


        historySearchViewModel.historySearchResults.observe(viewLifecycleOwner){ it ->

            navmenu.removeGroup(R.id.history_group)

            var i = 0
            if(it != null){
                for(historyItem in it.history){
                    historyVM.addHistoryItem(HistoryDatabaseItem(historyItem.history_item_id,
                    historyItem.voice_name,
                    historyItem.text,
                    historyItem.date_unix))


                    //Src: https://www.bestprog.net/en/2022/05/23/kotlin-anonymous-functions-lambda-expressions/

                    val textToUse = {str: String -> if(str.length < 30){
                        str
                    }
                    else{
                        str.substring(0,30) + "..."
                    }}

                    val group = navmenu.addSubMenu(R.id.history_group, Menu.NONE, Menu.NONE, historyItem.voice_name)
                    val menuItemClicked = group.add(Menu.NONE, Menu.NONE, Menu.NONE, textToUse(historyItem.text) )

                    val fragmentManager = fragmentManager

                    fragmentManager?.findFragmentById(R.id.history_by_selected_voice)

                    menuItemClicked.setOnMenuItemClickListener {

                        playFile(historyItem)

                        true
                    }

                    i+=1

                    //Show the last ten menu items in the history, by any character
                    if(i == 10){
                        break
                    }

                }


            }
        }

        val prefManager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        mediaViewModel = ViewModelProvider(requireActivity()).get(MediaViewModel::class.java)

        loadingIndicator = view.findViewById(R.id.voice_generator_loading_indicator)


        Log.d("get line is", getString(R.string.pref_quick_voice_key))
        val quickvoice = prefManager.getString(getString(R.string.pref_quick_voice_key), null)
        Log.d("quickvoice is", quickvoice!!)
        val idList : Array<out String> = resources.getStringArray(R.array.pref_quick_voice_values)
        Log.d("quickvoice is", quickvoice!!)
        val voiceList : Array<out String> = resources.getStringArray(R.array.pref_quick_voice_entries)
        Log.d("quickvoice is", quickvoice!!)
        val idx = voiceList.indexOf(quickvoice)
        Log.d("idx is", idx.toString())

        val quickid = idList[idx]
        Log.d("quickvoice is", quickvoice!!)



        newVoice = Voice(quickid, quickvoice!!, "junk")

        view.findViewById<TextView>(R.id.tv_selected_voice).text = getString(
            R.string.voice_name,
            newVoice!!.name
        )

        val editText = view.findViewById<TextView>(R.id.edit_generated_text)

        editText.setHint(getString(R.string.generate_hint, newVoice!!.name))

        val generateButton = view.findViewById<Button>(R.id.button_generate_text)

        generateButton.setOnClickListener {
            onGenerateButtonClick()
        }
    }

    private fun onGenerateButtonClick(){
        loadingIndicator.visibility = View.VISIBLE

        val userRequestedText: String =
            view?.findViewById<EditText>(R.id.edit_generated_text)?.text.toString()

        if (userRequestedText == "") {

            Log.d("userText", "user text is null")
            Snackbar.make(
                requireView(),
                "You have to enter some text in order to generate audio",
                Snackbar.LENGTH_LONG
            ).show()
            loadingIndicator.visibility = View.INVISIBLE
        }
        else{
            playGeneratedAudio(userRequestedText)
            currentlyGenerating = true
        }


    }

    private fun playGeneratedAudio(userRequestedText: String){

        loadingIndicator.visibility = View.VISIBLE

        if(currentlyGenerating){
            val toast = Toast.makeText(
                requireContext(),
                "Please wait until your current request has finished before generating another" +
                        "request",
                Toast.LENGTH_SHORT
            )
            toast.show()
            loadingIndicator.visibility = View.INVISIBLE
            return
        }

        coroutineScope.launch {
            withContext(Dispatchers.IO){


                try{

                    val audioFile = getAudio(
                        newVoice!!.voice_id,
                        generateJsonRequestBody(userRequestedText))
                    if(audioFile!!.isNotEmpty()){

                        val tmpMP3 = kotlin.io.path.createTempFile(newVoice?.name ?: "null", ".mp3")
                        tmpMP3.writeBytes(audioFile ?: byteArrayOf())

                        val file = File(tmpMP3.pathString)

                        filePath = tmpMP3.pathString

                        val fileUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )

                        mediaViewModel.mediaPlayer?.apply {
                            stop()
                            reset()
                            release()
                        }

                        mediaViewModel.mediaPlayer = MediaPlayer.create(requireContext(), fileUri)?.apply {

                            start()
                            mediaViewModel.isPlaying = true

                            setOnCompletionListener {
                                reset()
                                release()
                                mediaViewModel.mediaPlayer = null
                                mediaViewModel.isPlaying = false
                                currentlyGenerating = false
                            }
                        }


                    }
                } catch(e: Exception){
                    currentlyGenerating = false
                    Snackbar.make(
                        requireView(),
                        e.toString(),
                        Snackbar.LENGTH_LONG
                    ).show()
                }

            }
            loadingIndicator.visibility = View.INVISIBLE
        }

    }


    suspend fun getAudio(voice_id: String, requestBody: RequestBody): ByteArray? {

        val response = voiceservice.generateVoiceAudio(voice_id, requestBody).apply {
            delay(500)
        }

        return if(response.isSuccessful){
            response.body()?.bytes()}
        else{
            null}

    }



    private fun generateJsonRequestBody(text: String): RequestBody {

        val requestJSONObject = JSONObject()

        val voiceSettings = JSONObject()

        requestJSONObject.put("text", text)

        voiceSettings.put("stability", "0.75")
        voiceSettings.put("similarity_boost", "0.75")

        requestJSONObject.put("voice_settings", voiceSettings)

        return requestJSONObject.toString().toRequestBody("application/json".toMediaTypeOrNull())


    }

    private fun playFile(historyItem: HistoryItem){

        coroutineScope.launch {
            withContext(Dispatchers.IO){
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.elevenlabs.io/v1/history/${historyItem.history_item_id}/audio")
                    .header("xi-api-key", BuildConfig.ELEVEN_LABS_API)
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("$response")

                        val bytes = response.body?.bytes()

                        val tmpMP3 = kotlin.io.path.createTempFile("${historyItem.voice_name}", ".mp3")

                        val file = File(tmpMP3.pathString)

                        tmpMP3.writeBytes(bytes!!)

                        val fileUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )


                        mediaViewModel.mediaPlayer?.apply {
                            stop()
                            reset()
                            release()
                        }

                        mediaViewModel.mediaPlayer = MediaPlayer.create(requireContext(), fileUri)?.apply {
                            start()

                            mediaViewModel.isPlaying = true


                            setOnCompletionListener {
                                reset()
                                release()
                                mediaViewModel.mediaPlayer = null
                                mediaViewModel.isPlaying = false
                            }
                        }

                    }
                } catch(e: Exception){
                    Snackbar.make(
                        requireView(),
                        e.toString(),
                        Snackbar.LENGTH_LONG
                    ).show()

                }
            }

        }
    }

    override fun onPause() {
        super.onPause()
        mediaViewModel.mediaPlayer?.pause()
        mediaViewModel.isPlaying = false
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaViewModel.mediaPlayer?.apply {
            stop()
            reset()
            release()
        }
        mediaViewModel.mediaPlayer = null
        mediaViewModel.isPlaying = false
    }

}