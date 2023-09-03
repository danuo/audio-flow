package com.example.audio_meter

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class DataHandler(
    private val context: MainActivity,
    private val databaseHandler: DatabaseHandler,
    private val amplitudeTextView: TextView,
    private val audioMeterLayout: LinearLayout
) {
    private val nGroup: Int = 30  // every 3 seconds
    private var counter: Int = 0
    private var valSum: Float = 0f

    fun updateUI(amplitude: Int) {
        context.handler.post {
            addToMean(amplitude)

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

    private fun addToMean(amplitude: Int) {
        valSum += amplitude
        counter += 1
        if (counter == nGroup) {
            var avg = valSum / nGroup
            counter = 0
            valSum = 0f
            databaseHandler.insertData(avg)
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