package com.example.audio_meter

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class AudioRecorder(val audioRecord: AudioRecord) {
    var isRecording = false
    
    fun toggleRecording() {
        if (isRecording) {
            isRecording = false
            audioRecord.stop()
        } else {
            isRecording = true
            audioRecord.startRecording()
        }
    }


}