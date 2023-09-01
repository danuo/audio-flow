package com.example.audio_meter

import android.media.AudioRecord


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