package com.example.audio_meter

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs


class MainActivity : ComponentActivity() {

    private lateinit var amplitudeTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var audioMeterLayout: LinearLayout


    private lateinit var audioRecorder: AudioRecorder
    private lateinit var databaseHandler: DatabaseHandler
    private lateinit var dataHandler: DataHandler
    val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAudio()
        initUI()
        databaseHandler = initDB()
        CoroutineScope(Dispatchers.IO).launch {
            initServer(databaseHandler)
        }
        dataHandler = DataHandler(this, databaseHandler, amplitudeTextView, audioMeterLayout)
    }

    companion object {
        const val NLEDS = 10
        const val NLEDS_ORANGE = NLEDS - 1
        const val NLEDS_GREEN = NLEDS_ORANGE / 2
        private const val REFRESH_RATE = 10
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = (SAMPLE_RATE / REFRESH_RATE).toInt()  // before: 1024
    }

    private fun initServer(databaseHandler: DatabaseHandler) {
        val server = Server(this, databaseHandler)
        server.startServer()
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

    private fun initUI() {
        setContentView(R.layout.activity_main)
        amplitudeTextView = findViewById<TextView>(R.id.amplitudeText)
        tempTextView = findViewById<TextView>(R.id.tempText)
        audioMeterLayout = findViewById<LinearLayout>(R.id.audioMeterLayout)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            showConfirmationDialog()
        }
        val startRecordButton = findViewById<Button>(R.id.startButton)
        startRecordButton.setOnClickListener {
            audioRecorder.toggleRecording()
            if (audioRecorder.isRecording) {
                startRecordButton.text = "Stop Recording"
                getAmplitude()
            } else {
                startRecordButton.text = "Start Recording"
            }
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you want to delete all data?")
            .setCancelable(false)
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
                databaseHandler.deleteAll()
            }
            )
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                dialog.cancel()
            })
        val alert = builder.create();
        alert.setTitle("AlertDialogExample");
        alert.show();
    }

    private fun initDB(): DatabaseHandler {
        return DatabaseHandler(context = this, textView = tempTextView)
    }

    private fun getAmplitude() {
        Thread {
            val audioBuffer = ShortArray(BUFFER_SIZE)
            while (audioRecorder.isRecording) {
                audioRecorder.audioRecord.read(audioBuffer, 0, BUFFER_SIZE)
                val maxAmplitude = calculateMaxAmplitude(audioBuffer)
                dataHandler.updateUI(maxAmplitude)
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