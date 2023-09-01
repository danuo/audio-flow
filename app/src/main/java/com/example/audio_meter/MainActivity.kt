package com.example.audio_meter

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.random.Random


class MainActivity : ComponentActivity() {

    private lateinit var amplitudeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val nLeds = 10
    private val nLedsOragne = nLeds - 1
    private val nLedsGreen = nLedsOragne / 2
    private lateinit var audioRecorder: AudioRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        checkRecordPermission()

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audioRecorder = AudioRecorder(audioRecord)
        val startButton = findViewById<Button>(R.id.startButton)
        amplitudeTextView = findViewById<TextView>(R.id.amplitudeText)
        startButton.setOnClickListener {
            audioRecorder.toggleRecording()
            if (audioRecorder.isRecording) {
                startButton.text = "Stop Recording"
                updateVoiceLevel()
            } else {
                startButton.text = "Start Recording"
            }
        }
    }

    companion object {
        private const val REFRESH_RATE = 10
        private const val SAMPLE_RATE = 44100

        // private const val BUFFER_SIZE = 1024
        private const val BUFFER_SIZE = (SAMPLE_RATE / REFRESH_RATE).toInt()
    }

    private fun updateVoiceLevel() {
        Thread {
            val audioBuffer = ShortArray(BUFFER_SIZE)
            while (audioRecorder.isRecording) {
                audioRecorder.audioRecord.read(audioBuffer, 0, BUFFER_SIZE)
                val maxAmplitude = calculateMaxAmplitude(audioBuffer)
                updateUI(maxAmplitude)
            }
        }.start()
    }

    private fun calculateMaxAmplitude(audioBuffer: ShortArray): Int {
        var max = 0
        for (sample in audioBuffer) {
            val amplitude = abs(sample.toInt())
            if (amplitude > max) {
                max = amplitude
            }
        }
        return max
    }

    private fun updateUI(amplitude: Int) {
        handler.post {
            val randomInRange = Random.nextInt(1, 10000)
            val amplitudeText = "Amplitude: $amplitude random number: $randomInRange"
            amplitudeTextView.text = amplitudeText
            val amplitudeTemp = randomInRange
            val audioMeterLayout = findViewById<LinearLayout>(R.id.audioMeterLayout)
            val thresh: Int = amplitudeTemp / 1000
            for (index in 0 until nLeds) {
                val led = audioMeterLayout.getChildAt(index) as View
                if (index < thresh) {
                    led.setBackgroundColor(getColorForAudioLevelOn(amplitudeTemp))
                } else {
                    led.setBackgroundColor(getColorForAudioLevelOff(amplitudeTemp))
                }
            }
        }
    }

    private fun getColorForAudioLevelOn(amplitude: Int): Int {
        return if (amplitude > 10000) {
            0xFFFF0000.toInt() // red
        } else if (amplitude > 7000) {
            0xFFff4433.toInt() // orange
        } else {
            0xFF008000.toInt() // green
        }
    }

    private fun getColorForAudioLevelOff(amplitude: Int): Int {
        return if (amplitude > 10000) {
            0xFF400000.toInt() // red
        } else if (amplitude > 7000) {
            0xFF40110D.toInt() // orange
        } else {
            0xFF002000.toInt() // green
        }
    }

    private fun checkRecordPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }
}