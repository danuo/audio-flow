package com.example.audio_flow

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.databinding.DataBindingUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.audio_flow.databinding.ActivityMainBinding


//bugs:
//will start without microphone permission -> causes plenty of bugs
//notification will not respawn


class MainActivity : ComponentActivity() {
    private val application: MainApplication = MainApplication.getInstance()
    lateinit var databaseHandler: DataHandler
    lateinit var uiHandler: UiHandler
    val handler = Handler(Looper.getMainLooper())

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val ledDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val maxAmplitudeDbu = intent?.getDoubleExtra("maxAmplitudeDbu", 20.0)?.toInt()
            val rmsAmplitudeDbu = intent?.getDoubleExtra("rmsAmplitudeDbu", 20.0)?.toInt()
            Log.d("MainActivity", "ledDataReceiver, ${intent?.action} $maxAmplitudeDbu")
            if ((maxAmplitudeDbu is Int) and (rmsAmplitudeDbu is Int)) {
                uiHandler.updateUI(
                    mapOf(
                        "maxAmplitudeDbu" to maxAmplitudeDbu!!,
                        "rmsAmplitudeDbu" to rmsAmplitudeDbu!!
                    )
                )
            }
        }
    }

    private val uiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "${intent?.action}")
            uiHandler.updateButtons()
        }
    }

    companion object {
        const val REFRESH_RATE = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "now")

        _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        uiHandler = UiHandler(this, binding)
        databaseHandler = DataHandler(context = this, uiHandler = uiHandler)
        getPermissionsNotification()
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(application).registerReceiver(
            ledDataReceiver,
            IntentFilter("ledData")
        )
        LocalBroadcastManager.getInstance(application).registerReceiver(
            uiReceiver,
            IntentFilter("updateUi")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(application)
            .unregisterReceiver(ledDataReceiver)
        LocalBroadcastManager.getInstance(application)
            .unregisterReceiver(uiReceiver)
    }

    private fun getPermissionsNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        // no user interaction needed?
        var test: Boolean? = null
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // permission is given
            test = true
        }
        Log.d("MainActivity", "A: notification permission is: $test")

        getPermissionAudio()
    }

    private fun getPermissionAudio() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // permission is given
            application.permissionAudio = true
        } else {
            val requestPermissionLauncher =
                this.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    application.permissionAudio = isGranted
                    Log.d("MainActivity", "B: audio permission is: ${application.permissionAudio}")
                }
            requestPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
        Log.d("MainActivity", "A: audio permission is: ${application.permissionAudio}")
    }


}