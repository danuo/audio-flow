package com.example.audio_meter

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

const val N_LEDS = 10
const val N_LEDS_ORANGE = N_LEDS - 1
const val N_LEDS_GREEN = N_LEDS_ORANGE / 2

class UiHandler(
    private val context: MainActivity,
    private val amplitudeTextView: TextView,
    private val audioMeterLayout: LinearLayout
) {

    private var amplitude: Int = 10
    private var nSamples: Int = 0

    fun updateUI(data: Map<String, Int>) {
        if (data.containsKey("amplitude")) {
            amplitude = data["amplitude"]!!
        }
        if (data.containsKey("nSamples")) {
            nSamples = data["nSamples"]!!
        }

        updateText()
        updateLeds()
    }

    private fun updateText() {
        context.handler.post {
            val outText = "Amplitude: $amplitude, nSamples: $nSamples"
            amplitudeTextView.text = outText
        }
    }

    private fun updateLeds() {
        context.handler.post {
            val outText = "Amplitude: $amplitude, nSamples: $nSamples"
            amplitudeTextView.text = outText
        }

        val thresh: Int = amplitude / 1000
        for (index in 0 until N_LEDS) {
            val led = audioMeterLayout.getChildAt(index) as View
            if (index < thresh) {
                led.setBackgroundColor(getColorForAudioLevelOn(amplitude))
            } else {
                led.setBackgroundColor(getColorForAudioLevelOff(amplitude))
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