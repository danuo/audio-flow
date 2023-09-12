package com.example.audio_meter

import android.content.Context
import android.Manifest
import android.content.BroadcastReceiver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class MainActivity : ComponentActivity() {
    private val application: MainApplication = MainApplication.getInstance()
    lateinit var databaseHandler: DatabaseHandler
    lateinit var uiHandler: UiHandler
    val handler = Handler(Looper.getMainLooper())
    var counter = 0

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val maxAmplitudeDbu = intent?.getDoubleExtra("maxAmplitudeDbu", 20.0)?.toInt()
            val rmsAmplitudeDbu = intent?.getDoubleExtra("rmsAmplitudeDbu", 20.0)?.toInt()
            val threadId = intent?.getLongExtra("threadId", 0)
            if ((maxAmplitudeDbu is Int) and (rmsAmplitudeDbu is Int)) {
                counter += 1
                Log.d("mainactivity", "${maxAmplitudeDbu.toString()} $counter $threadId")
                uiHandler.updateUI(
                    mapOf(
                        "maxAmplitudeDbu" to maxAmplitudeDbu!!,
                        "rmsAmplitudeDbu" to rmsAmplitudeDbu!!
                    )
                )
            }
        }
    }

    companion object {
        const val REFRESH_RATE = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "now")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uiHandler = UiHandler(this)
        databaseHandler = DatabaseHandler(context = this, uiHandler = uiHandler)
        // startServerOld(this, databaseHandler)
        getPermissionsNotification()
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter("ledData")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
    }

    private fun getPermissionsNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        getPermissionAudio()
    }

    private fun getPermissionAudio() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startService()
        } else {
            val requestPermissionLauncher =
                this.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        startService()
                    }
                }

            requestPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startService() {
        val intent = Intent(applicationContext, ServerService::class.java)
        val htmlString =
            loadHtmlResourceToString(context = this, R.raw.index).trimIndent()
        intent.putExtra("html", htmlString)
        intent.action = "start"
        startService(intent)
    }

    fun updateThing() {
        val intent = Intent(applicationContext, ServerService::class.java)
        intent.action = "refresh"
        startService(intent)
        if ((!application.wifiOn) and (!application.recordingOn)) {
//            stopThing()
        }
    }

    private fun stopThing() {
        Log.d("MainActivity", "stopping service")
        val intent = Intent(applicationContext, ServerService::class.java)
        intent.action = "stop"
        startService(intent)
    }


    private fun startServerOld(context: MainActivity, databaseHandler: DatabaseHandler) {
        CoroutineScope(Dispatchers.IO).launch {
            ServerOld(context, databaseHandler)
        }
    }

    private fun loadHtmlResourceToString(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        return inputStream.readBytes().toString(Charsets.UTF_8)
    }
}