package com.friday.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var listenButton: Button

    private val recordAudioRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        commandText = findViewById(R.id.commandText)
        listenButton = findViewById(R.id.listenButton)

        tts = TextToSpeech(this, this)

        listenButton.setOnClickListener {
            checkAudioPermissionAndListen()
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "FRIDAY Ready"
        } else {
            statusText.text = "Speech recognition unavailable"
            listenButton.isEnabled = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    private fun checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioRequestCode
            )
        } else {
            initAndStartListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == recordAudioRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAndStartListening()
        } else {
            statusText.text = "Microphone permission required"
        }
    }

    private fun initAndStartListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                statusText.text = "FRIDAY is listening"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                val command = matches?.firstOrNull()

                if (!command.isNullOrBlank()) {
                    processFridayCommand(command)
                }

                statusText.text = "Ready"
            }

            override fun onError(error: Int) {
                statusText.text = "Error code: $error. Try again."
            }

            override fun onEndOfSpeech() {
                statusText.text = "Processing..."
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(speechIntent)
    }

    private fun processFridayCommand(command: String) {
        commandText.text = "You: $command"

        val lower = command.lowercase(Locale.getDefault())

        val reply = when {
            lower.contains("hello") ||
            lower.contains("नमस्ते") ||
            lower.contains("हेलो") ->
                "नमस्ते! मैं FRIDAY हूँ।"

            lower.contains("time") ||
            lower.contains("समय") ->
                "अभी का समय आपके फोन के अनुसार है।"

            lower.contains("who are you") ||
            lower.contains("तुम कौन") ->
                "मैं FRIDAY voice assistant हूँ।"

            else ->
                "मैंने सुना: $command"
        }

        speak(reply)
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "FRIDAY_REPLY"
            )
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
