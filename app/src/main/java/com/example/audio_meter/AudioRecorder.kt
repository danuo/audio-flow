package com.example.audio_meter

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs


class AudioRecorder(
    private val context: MainActivity
) {
    var isRecording = false
    private val audioRecord: AudioRecord

    private val nGroup: Int = 30  // every 3 seconds
    private var counter: Int = 0
    private var valSum: Float = 0f

    init {
        checkRecordPermission()
        val minBufferSize = AudioRecord.getMinBufferSize(
            MainActivity.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            MainActivity.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }

    fun toggleRecording() {
        if (isRecording) {
            isRecording = false
            audioRecord.stop()
        } else {
            isRecording = true
            audioRecord.startRecording()
        }
    }

    fun readAudioData() {
        Thread {
            val audioBuffer = ShortArray(MainActivity.BUFFER_SIZE)
            while (this.isRecording) {
                audioRecord.read(audioBuffer, 0, MainActivity.BUFFER_SIZE)
                val maxAmplitude = calculateMaxAmplitude(audioBuffer)
                processAmplitude(maxAmplitude)
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

    private fun processAmplitude(amplitude: Int) {
        context.uiHandler.updateUI(mapOf("amplitude" to amplitude))
        poolData(amplitude)
    }

    private fun poolData(amplitude: Int) {
        valSum += amplitude
        counter += 1
        if (counter == nGroup) {
            val avg = valSum / nGroup
            context.databaseHandler.insertData(avg)
            counter = 0
            valSum = 0f
        }
    }

    private fun checkRecordPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }
}