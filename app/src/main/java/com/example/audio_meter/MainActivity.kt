package com.example.audio_meter

import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class MainActivity : ComponentActivity() {

    private lateinit var audioRecord: AudioRecord
    private var isRecording = false
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize audioRecord with appropriate settings
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        startButton.setOnClickListener {
            isRecording = true
            audioRecord.startRecording()
            updateVoiceLevel()
        }

        stopButton.setOnClickListener {
            isRecording = false
            audioRecord.stop()
        }
    }

    private fun updateVoiceLevel() {
        Thread {
            val audioBuffer = ShortArray(BUFFER_SIZE)
            while (isRecording) {
                audioRecord.read(audioBuffer, 0, BUFFER_SIZE)
                val maxAmplitude = calculateMaxAmplitude(audioBuffer)
                updateUI(maxAmplitude)
            }
        }.start()
    }

    private fun calculateMaxAmplitude(audioBuffer: ShortArray): Int {
        var max = 0
        for (sample in audioBuffer) {
            val amplitude = Math.abs(sample.toInt())
            if (amplitude > max) {
                max = amplitude
            }
        }
        return max
    }

    private fun updateUI(amplitude: Int) {
        handler.post {
            // Update your UI elements (e.g., progress bar, waveform) with amplitude
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 1024
    }
}