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

    private val drawables: Map<String, List<Drawable>>
    private val dbThresholds: List<Float>
    private val extraOptionsLayout: LinearLayout
    private var extraOptionsVisible = true

    init {
        dbThresholds = getDbThresholds(startValue = 20, step = -2, nValues = N_LEDS)
        drawables = generateDrawables()
        initLeds()
        initChart()
        extraOptionsLayout = context.findViewById(R.id.extraOptions)
        applyVisibility()
        val toggleButton: Button = context.findViewById(R.id.toggleButton)
        toggleButton.setOnClickListener {
            extraOptionsVisible = !extraOptionsVisible
            applyVisibility()
        }
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

    private fun applyVisibility() {
        if (extraOptionsVisible) {
            extraOptionsLayout.visibility = View.VISIBLE
        } else {
            extraOptionsLayout.visibility = View.GONE
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

        chart.isScaleXEnabled = true
        chart.isScaleYEnabled = true
        chart.setPinchZoom(true)
        // disable description text
        chart.description.isEnabled = false

        val xAxis = chart.xAxis

        // vertical grid lines
        xAxis.enableGridDashedLine(10f, 10f, 0f)

        // disable dual axis (only use LEFT axis)
        chart.axisRight.isEnabled = false

        val yAxis = chart.axisLeft
        // yAxis.axisMaximum = 200f
        // yAxis.axisMinimum = -50f

        // dark mode
        // https://github.com/PhilJay/MPAndroidChart/issues/5015
        // chart.setBackgroundColor(Color.BLACK)

        chart.legend.isEnabled = false

        chart.invalidate()
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

    private fun initLeds() {
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

    private fun getDbThresholds(startValue: Int, step: Int, nValues: Int): List<Float> {
        val dbThresholds = mutableListOf<Float>()
        var currentValue = startValue

        repeat(nValues) {
            dbThresholds.add(currentValue.toFloat())
            currentValue += step
        }
        return dbThresholds.reversed()
    }

    private fun updateLeds() {
        context.handler.post {
            val outText = "Amplitude: $amplitude, nSamples: $nSamples"
            amplitudeTextView.text = outText
        }

        for (index in 0 until N_LEDS) {
            val thresh = dbThresholds[index]
            val led = audioMeterLayout.getChildAt(N_LEDS - 1 - index) as View
            led.background = getDrawable(amplitude.toFloat(), thresh)
        }
    }


    private fun getDrawable(amplitude: Float, thresh: Float): Drawable {
        val redThresh = 14.0f
        val orangeThresh = 8.0f
        val ledOn = amplitude > thresh
        val key = if (ledOn) {
            "on"
        } else {
            "off"
        }
        val colorIndex = if (thresh >= redThresh) {
            2
        } else if (thresh >= orangeThresh) {
            1
        } else {
            0
        }
        return drawables[key]!![colorIndex]
    }

    private fun generateDrawables(): Map<String, List<Drawable>> {
        val rgbColors = listOf<Int>(Color.GREEN, Color.argb(1.0f, 1.0f, 0.65f, 0.0f), Color.RED)
        return mapOf(
            "on" to rgbColors.map { createDrawable(it) },
            "off" to rgbColors.map { darkenColor(it, 0.3) }.map { createDrawable(it) }
        )
    }

    private fun createDrawable(color: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        drawable.setStroke(4, Color.BLACK)
        return drawable
    }

    private fun darkenColor(colorInt: Int, factor: Double): Int {
        val red = (Color.red(colorInt) * factor).toInt()
        val green = (Color.green(colorInt) * factor).toInt()
        val blue = (Color.blue(colorInt) * factor).toInt()
        return Color.argb(255, red, green, blue)
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