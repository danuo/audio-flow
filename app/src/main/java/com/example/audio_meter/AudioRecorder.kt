package com.example.audio_meter

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt


class AudioRecorder(
    private val context: MainActivity
) {
    var isRecording = false
    private val audioRecord: AudioRecord

    private val nGroup: Int = 30  // every 3 seconds
    private var counter: Int = 0
    private var valSquareSum: Double = 0.0

    init {
        checkRecordPermission()
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
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = (SAMPLE_RATE / MainActivity.REFRESH_RATE)  // before: 1024
    }

    private fun valToDbu(rms: Float): Float {
        // factor 2 = 6 dB
        // factor 0.5 = -6 dB
        // rms of 32000 = 20 dBu
        return 20 * log10(0.00001 + rms.toDouble()).toFloat()
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
            val audioBuffer = ShortArray(BUFFER_SIZE)
            while (this.isRecording) {
                audioRecord.read(audioBuffer, 0, BUFFER_SIZE)
                val maxAmplitude = calculateMaxAmplitude(audioBuffer)
                processAmplitude(maxAmplitude)
            }
        }.start()
    }

    private fun calculateMaxAmplitude(audioBuffer: ShortArray): Int {
        return audioBuffer.maxOrNull()!!.toInt()
    }

    private fun processAmplitude(amplitude: Int) {
        val amplitudeDbu = valToDbu(amplitude.toFloat())
        context.uiHandler.updateUI(
            mapOf(
                "amplitude" to amplitude,
                "amplitudeDbu" to amplitudeDbu.toInt()
            )
        )
        poolData(amplitude)
    }

    private fun poolData(amplitude: Int) {
        valSquareSum += amplitude.toDouble().pow(2)
        counter += 1
        if (counter == nGroup) {
            val valSquareAvg = valSquareSum / nGroup
            val valRMS = sqrt(valSquareAvg).toFloat()
            val valRMSdB = valToDbu(valRMS)
            val time: Long = System.currentTimeMillis()
            context.databaseHandler.insertData(time, valRMSdB)
            valSquareSum = 0.0
            counter = 0
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