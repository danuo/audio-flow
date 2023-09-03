package com.example.audio_meter

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

const val N_LEDS = 10
const val N_LEDS_ORANGE = N_LEDS - 1
const val N_LEDS_GREEN = N_LEDS_ORANGE / 2

class UiHandler(
    private val context: MainActivity,
    private val lineChart: LineChart,
    private val amplitudeTextView: TextView,
    private val audioMeterLayout: LinearLayout
) {

    private var amplitude: Int = 10
    private var nSamples: Int = 0


    fun updateChart(data: List<Value>) {
        val dataSet =
            LineDataSet(data.map { Entry(it.time.toFloat(), it.value) }, "Temperature")


        // Customize the appearance of the graph (e.g., line color, points, etc.)
        dataSet.color = (0xFFFF0000).toInt()
        dataSet.setCircleColor((0xFFFF0000).toInt())

        // Create a LineData object and set the data set
        val lineData = LineData(dataSet)

        // Set the LineData to your LineChart
        lineChart.data = lineData

        // Customize the LineChart further if needed

        // Refresh the chart
        lineChart.invalidate()
    }


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