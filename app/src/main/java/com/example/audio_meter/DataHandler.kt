package com.example.audio_meter

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class DataHandler(
    private val context: MainActivity, private val amplitudeTextView: TextView,
    private val audioMeterLayout: LinearLayout
) {


    fun updateUI(amplitude: Int) {
        context.handler.post {
            val amplitudeText = "Amplitude: $amplitude"
            amplitudeTextView.text = amplitudeText

            val thresh: Int = amplitude / 1000
            for (index in 0 until MainActivity.NLEDS) {
                val led = audioMeterLayout.getChildAt(index) as View
                if (index < thresh) {
                    led.setBackgroundColor(getColorForAudioLevelOn(amplitude))
                } else {
                    led.setBackgroundColor(getColorForAudioLevelOff(amplitude))
                }
            }
        }
    }

    private fun getColorForAudioLevelOn(amplitude: Int): Int {
        return if (amplitude > 10000) {
            0xFFFF0000.toInt() // red
        } else if (amplitude > 7000) {
            0xFFff4433.toInt() // orange
        } else {
            0xFF008000.toInt() // green
        }
    }

    private fun getColorForAudioLevelOff(amplitude: Int): Int {
        return if (amplitude > 10000) {
            0xFF400000.toInt() // red
        } else if (amplitude > 7000) {
            0xFF40110D.toInt() // orange
        } else {
            0xFF002000.toInt() // green
        }
    }
}