package com.example.audio_meter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Resources
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Date
import java.text.SimpleDateFormat
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable


const val N_LEDS = 28
const val N_LEDS_ORANGE = N_LEDS - 1
const val N_LEDS_GREEN = N_LEDS_ORANGE / 2
const val SHIFT = 1693777500000L

@SuppressLint("SetTextI18n")
class UiHandler(
    private val context: MainActivity,
) {

    private val amplitudeTextView: TextView = context.findViewById(R.id.amplitudeText)
    private val chart: LineChart = context.findViewById(R.id.lineChart)
    private val audioMeterLayout: LinearLayout = context.findViewById((R.id.audioMeterLayout))
    private var amplitude: Int = 10
    private var nSamples: Int = 0

    init {
        initVolumeMeter()
        initChart()
        val deleteButton = context.findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            showConfirmationDialog()
        }
        val startRecordButton = context.findViewById<Button>(R.id.startButton)
        startRecordButton.setOnClickListener {
            context.audioRecorder.toggleRecording()
            if (context.audioRecorder.isRecording) {
                startRecordButton.text = "Stop Recording"
                context.audioRecorder.readAudioData()
            } else {
                startRecordButton.text = "Start Recording"
            }
        }
    }

    private fun initChart() {
        chart.xAxis.valueFormatter = LineChartXAxisValueFormatter()
    }

    fun updateChart(data: List<Value>) {
        val dataSet =
            LineDataSet(data.map { Entry((it.time - SHIFT).toFloat(), it.value) }, "Temperature")
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.color = 0xFFFF0000.toInt()
        val lineData = LineData(dataSet)
        chart.data = lineData

        chart.isScaleXEnabled = true;
        chart.isScaleYEnabled = true;
        chart.setPinchZoom(true);
        // disable description text
        chart.description.isEnabled = false;

        val xAxis = chart.xAxis;

        // vertical grid lines
        xAxis.enableGridDashedLine(10f, 10f, 0f);

        // disable dual axis (only use LEFT axis)
        chart.axisRight.isEnabled = false;

        val yAxis = chart.axisLeft
        // yAxis.axisMaximum = 200f;
        // yAxis.axisMinimum = -50f;

        // darkmode
        // https://github.com/PhilJay/MPAndroidChart/issues/5015
        // chart.setBackgroundColor(Color.BLACK)

        chart.legend.isEnabled = false

        chart.invalidate()
    }

    private fun initVolumeMeter() {
        val audioMeterLayout = context.findViewById<LinearLayout>(R.id.audioMeterLayout)

        val width = (Resources.getSystem().displayMetrics.density * 50).toInt()

        val layoutParams = LinearLayout.LayoutParams(width, 0)
        layoutParams.weight = 1f

        for (i in 0 until N_LEDS) {
            val view = View(context)
            view.layoutParams = layoutParams
            view.setBackgroundColor((0xFF000000).toInt())
            audioMeterLayout.addView(view)
        }
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
            val led = audioMeterLayout.getChildAt(N_LEDS - 1 - index) as View
            if (index < thresh) {
                // led.setBackgroundColor(getColorForAudioLevelOn(amplitude))
                led.background = getDrawable(getColorForAudioLevelOn(amplitude))
            } else {
                // led.setBackgroundColor(getColorForAudioLevelOff(amplitude))
                led.background = getDrawable(getColorForAudioLevelOff(amplitude))
            }
        }
    }

    private fun getDrawable(color: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        drawable.setStroke(4, Color.BLACK)
        return drawable
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Do you want to delete all data?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { dialog, _ ->
                dialog.dismiss()
                context.databaseHandler.deleteAll()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.setTitle("AlertDialogExample")
        alert.show()
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


class LineChartXAxisValueFormatter : IndexAxisValueFormatter() {
    @SuppressLint("SimpleDateFormat")
    override fun getFormattedValue(value: Float): String {
        val emissionsMilliSince1970Time = value.toLong() + SHIFT
        val timeMilliseconds = Date(emissionsMilliSince1970Time)
        val dateFormat = SimpleDateFormat("HH:mm")
        return dateFormat.format(timeMilliseconds)
    }
}