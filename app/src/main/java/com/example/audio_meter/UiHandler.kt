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
import android.view.Gravity


const val N_LEDS = 28
const val TIME_SHIFT = 1693777500000L

@SuppressLint("SetTextI18n")
class UiHandler(
    private val context: MainActivity,
) {

    private val amplitudeTextView: TextView = context.findViewById(R.id.amplitudeText)
    private val chart: LineChart = context.findViewById(R.id.lineChart)
    private val audioMeterLayout: LinearLayout = context.findViewById((R.id.audioMeterLayout))
    private var amplitude: Int = 10
    private var amplitudeDbu: Float = 10f
    private var effectiveAmplitudeDbu: Float = 10f
    private var nSamples: Int = 0

    private val drawables: Map<String, List<Drawable>>
    private val dbThresholds: List<Float>
    private val optionsLayout: LinearLayout
    private val debugLayout: LinearLayout
    private var optionsLayoutVisible = true
    private var debugLayoutVisible = true

    init {
        dbThresholds = getDbThresholds(startValue = 20, step = -2, nValues = N_LEDS)
        drawables = generateDrawables()
        initLeds()
        initChart()
        debugLayout = context.findViewById(R.id.debugLayout)
        optionsLayout = context.findViewById(R.id.optionsLayout)
        applyVisibility()
        val toggleOptionsButton: Button = context.findViewById(R.id.toggleOptionsButton)
        toggleOptionsButton.setOnClickListener {
            optionsLayoutVisible = !optionsLayoutVisible
            applyVisibility()
        }
        val toggleDebugButton: Button = context.findViewById(R.id.toggleDebugButton)
        toggleDebugButton.setOnClickListener {
            debugLayoutVisible = !debugLayoutVisible
            applyVisibility()
        }
        val deleteButton = context.findViewById<Button>(R.id.deleteDataButton)
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
        if (debugLayoutVisible) {
            debugLayout.visibility = View.VISIBLE
        } else {
            debugLayout.visibility = View.GONE
        }
        if (optionsLayoutVisible) {
            optionsLayout.visibility = View.VISIBLE
        } else {
            optionsLayout.visibility = View.GONE
        }
    }

    private fun initChart() {
        chart.xAxis.valueFormatter = LineChartXAxisValueFormatter()
    }

    fun updateChart(data: List<Value>) {
        val dataSet =
            LineDataSet(data.map {
                Entry(
                    (it.time - TIME_SHIFT).toFloat(),
                    it.value + context.dbShift
                )
            }, "Temperature")
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
        yAxis.axisMaximum = dbThresholds.last()
        yAxis.axisMinimum = dbThresholds.first()

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
        if (data.containsKey("amplitudeDbu")) {
            amplitudeDbu = data["amplitudeDbu"]!!.toFloat()
            effectiveAmplitudeDbu = amplitudeDbu + context.dbShift
        }
        if (data.containsKey("nSamples")) {
            nSamples = data["nSamples"]!!
        }

        updateText()
        updateLeds()
    }

    private fun updateText() {
        context.handler.post {
            val outText = "Amp: $amplitude, AmpdBu: $effectiveAmplitudeDbu, nSamples: $nSamples"
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
        val audioMeterLayoutText = context.findViewById<LinearLayout>(R.id.audioMeterLayoutText)
        val widthView = (Resources.getSystem().displayMetrics.density * 40).toInt()
        val widthText = (Resources.getSystem().displayMetrics.density * 30).toInt()
        val layoutParamsView = LinearLayout.LayoutParams(widthView, 0)
        layoutParamsView.weight = 1f
        val layoutParamsText = LinearLayout.LayoutParams(widthText, 0)
        layoutParamsText.weight = 1f

        for (i in 0 until N_LEDS) {
            val view = View(context)
            view.layoutParams = layoutParamsView
            view.setBackgroundColor((0xFF000000).toInt())
            audioMeterLayout.addView(view)

            if (i % 2 == 0) {
                val thresh = dbThresholds[N_LEDS - 1 - i]
                val text = TextView(context)
                text.layoutParams = layoutParamsText
                text.text = "$thresh"
                text.textSize = 12f
                text.gravity = Gravity.END
                audioMeterLayoutText.addView(text)
            }
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
            for (index in 0 until N_LEDS) {
                val thresh = dbThresholds[index]
                val led = audioMeterLayout.getChildAt(N_LEDS - 1 - index) as View
                led.background = getDrawable(effectiveAmplitudeDbu, thresh)
            }
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
        val emissionsMilliSince1970Time = value.toLong() + TIME_SHIFT
        val timeMilliseconds = Date(emissionsMilliSince1970Time)
        val dateFormat = SimpleDateFormat("HH:mm")
        return dateFormat.format(timeMilliseconds)
    }
}