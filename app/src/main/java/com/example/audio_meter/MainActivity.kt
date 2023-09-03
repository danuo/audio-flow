package com.example.audio_meter

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs


class MainActivity : ComponentActivity() {

    lateinit var audioRecorder: AudioRecorder
    lateinit var databaseHandler: DatabaseHandler
    private lateinit var uiHandler: UiHandler
    val handler = Handler(Looper.getMainLooper())
    private val nGroup: Int = 30  // every 3 seconds
    private var counter: Int = 0
    private var valSum: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAudio()
        setContentView(R.layout.activity_main)
        uiHandler = UiHandler(this)
        initDB(uiHandler)
        CoroutineScope(Dispatchers.IO).launch {
            initServer(databaseHandler)
        }
    }

    companion object {
        private const val REFRESH_RATE = 10
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = (SAMPLE_RATE / REFRESH_RATE)  // before: 1024
    }

    private fun initAudio() {
        checkRecordPermission()
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
        audioRecorder = AudioRecorder(audioRecord)
    }

    private fun initDB(uiHandler: UiHandler) {
        databaseHandler =
            DatabaseHandler(context = this, uiHandler = uiHandler)
    }

    private fun initServer(databaseHandler: DatabaseHandler) {
        val server = Server(this, databaseHandler)
        server.startServer()
    }

    fun readAudioData() {
        Thread {
            val audioBuffer = ShortArray(BUFFER_SIZE)
            while (audioRecorder.isRecording) {
                audioRecorder.audioRecord.read(audioBuffer, 0, BUFFER_SIZE)
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
        uiHandler.updateUI(mapOf("amplitude" to amplitude))
        poolData(amplitude)
    }

    private fun poolData(amplitude: Int) {
        valSum += amplitude
        counter += 1
        if (counter == nGroup) {
            val avg = valSum / nGroup
            databaseHandler.insertData(avg)
            counter = 0
            valSum = 0f
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