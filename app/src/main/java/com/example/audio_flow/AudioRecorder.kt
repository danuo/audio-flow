package com.example.audio_flow

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt


class AudioRecorder(
    private val context: Service
) {
    private val application: MainApplication = MainApplication.getInstance()
    private var audioRecord: AudioRecord? = null

    private val nGroup: Int = 30  // every 3 seconds
    private var counter: Int = 0
    private var maxAmpList: List<Double> = listOf()
    private var rmsAmpSquareSum: Double = 0.0
    private val databaseAudio: AudioDatabaseThing = AudioDatabaseThing()
    private var recordingThread: Thread? = null
    var sendTime = System.currentTimeMillis()

    init {
        initAudio()
    }


    private fun initAudio() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
        }
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = (SAMPLE_RATE / MainActivity.REFRESH_RATE)  // before: 1024
    }


    fun startRecordingThread() {
        if (application.recordingOn) {
            audioRecord?.startRecording()
        }
        if (recordingThread?.state == Thread.State.RUNNABLE) {
            // do nothing
        } else {
            recordingThread = Thread {
                val audioBuffer = ShortArray(BUFFER_SIZE)
                while (application.recordingOn) {
                    audioRecord?.read(audioBuffer, 0, BUFFER_SIZE)
                    processAudioBuffer(audioBuffer)
                }
                audioRecord?.stop()
                sendLedData(-200.0, -200.0)
            }
            recordingThread?.start()
        }
    }

    private fun processAudioBuffer(audioBuffer: ShortArray) {
        val maxAmplitude = calcMaxAmplitude(audioBuffer)
        val rmsAmplitude = calcRmsAmplitude((audioBuffer))
        val maxAmplitudeDbu = valToDbu(maxAmplitude)
        val rmsAmplitudeDbu = valToDbu(rmsAmplitude)

        val nowTime = System.currentTimeMillis()
        // every 80 ms does not work
        if (nowTime - sendTime > 80) {
            sendTime = nowTime
            sendLedData(maxAmplitudeDbu, rmsAmplitudeDbu)
            poolData(maxAmplitude, rmsAmplitude)
        }
    }

    private fun calcMaxAmplitude(audioBuffer: ShortArray): Double {
        return audioBuffer.maxOf { abs(it.toDouble()) }
    }

    private fun calcRmsAmplitude(audioBuffer: ShortArray): Double {
        return sqrt(audioBuffer.map { it.toDouble().pow(2) }.average())
    }

    private fun valToDbu(rms: Double): Double {
        // factor 2 = 6 dB
        // factor 0.5 = -6 dB
        // rms of 32000 = 20 dBu
        return 20 * log10(0.00001 + rms)
    }

    private fun sendLedData(maxAmplitudeDbu: Double, rmsAmplitudeDbu: Double) {
        val intent = Intent("ledData")
        intent.putExtra("maxAmplitudeDbu", maxAmplitudeDbu)
        intent.putExtra("rmsAmplitudeDbu", rmsAmplitudeDbu)
        LocalBroadcastManager.getInstance(application).sendBroadcast(intent)
    }

    private fun poolData(maxAmplitude: Double, rmsAmplitude: Double) {
        maxAmpList += maxAmplitude
        rmsAmpSquareSum += rmsAmplitude.pow(2)
        counter += 1
        if (counter == nGroup) {
            val maxAmp = maxAmpList.max()
            val maxAmpDbu = valToDbu(maxAmp)
            val rmsAmpSquareAvg = rmsAmpSquareSum / nGroup
            val rmsAmp = sqrt(rmsAmpSquareAvg)
            val rmsAmpDbu = valToDbu(rmsAmp)
            val time: Long = System.currentTimeMillis()
            databaseAudio.insertData(
                time = time,
                maxAmpDbu = maxAmpDbu.toFloat(),
                rmsAmpDbu = rmsAmpDbu.toFloat()
            )
            maxAmpList = listOf()
            rmsAmpSquareSum = 0.0
            counter = 0
        }
    }

}

class AudioDatabaseThing {
    private val viewModel = ValueViewModel()

    fun insertData(time: Long, maxAmpDbu: Float, rmsAmpDbu: Float) {
        viewModel.insert(Value(time = time, maxAmpDbu = maxAmpDbu, rmsAmpDbu = rmsAmpDbu))
    }
}