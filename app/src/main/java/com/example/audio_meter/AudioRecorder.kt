package com.example.audio_meter

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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

    //    var isRecording = false
    private var audioRecord: AudioRecord? = null

    private val nGroup: Int = 30  // every 3 seconds
    private var counter: Int = 0
    private var valSquareSum: Double = 0.0

    private val databaseAudio: AudioDatabaseThing = AudioDatabaseThing()

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

    private fun valToDbu(rms: Float): Float {
        // factor 2 = 6 dB
        // factor 0.5 = -6 dB
        // rms of 32000 = 20 dBu
        return 20 * log10(0.00001 + rms.toDouble()).toFloat()
    }


    fun startRecordingThread() {
        Thread {
            if (application.recordingOn) {
                audioRecord?.startRecording()
            }
            val audioBuffer = ShortArray(BUFFER_SIZE)
            while (application.recordingOn) {
                audioRecord?.read(audioBuffer, 0, BUFFER_SIZE)
                val maxAmplitude = calculateMaxAmplitude(audioBuffer)
                processAmplitude(maxAmplitude)
            }
            audioRecord?.stop()
        }.start()
    }

    private fun calculateMaxAmplitude(audioBuffer: ShortArray): Int {
        // return audioBuffer.maxOf { abs(it.toInt()) }
        return audioBuffer.max().toInt()
    }

    private fun processAmplitude(amplitude: Int) {
        val amplitudeDbu = valToDbu(amplitude.toFloat())
//        context.uiHandler.updateUI(
//            mapOf(
//                "amplitude" to amplitude,
//                "amplitudeDbu" to amplitudeDbu.toInt()
//            )
//        )
        sendNotification(amplitude, amplitudeDbu.toInt())
        poolData(amplitude)
    }

    private fun sendNotification(amplitude: Int, amplitudeDbu: Int) {
        val intent = Intent("customtextforme")
        intent.putExtra("amplitude", amplitude)
        intent.putExtra("amplitudeDbu", amplitudeDbu)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun poolData(amplitude: Int) {
        valSquareSum += amplitude.toDouble().pow(2)
        counter += 1
        if (counter == nGroup) {
            val valSquareAvg = valSquareSum / nGroup
            val valRMS = sqrt(valSquareAvg).toFloat()
            val valRMSdB = valToDbu(valRMS)
            val time: Long = System.currentTimeMillis()
            databaseAudio.insertData(time, valRMSdB)
            valSquareSum = 0.0
            counter = 0
        }
    }

}

class AudioDatabaseThing() {
    private val database = ValueDatabase.getDatabase()
    private val repository = ValueRepository(database!!.valueDao())
    private val viewModel = ValueViewModel(repository)

    fun insertData(time: Long, value: Float) {
        viewModel.insert(Value(time = time, value = value))
    }
}