package com.example.audio_flow

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent

import android.content.res.Resources
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Random
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.EditText
import kotlin.math.roundToInt


const val N_LEDS = 32
const val TIME_SHIFT = 1693777500000L
const val DB_HIGH = 20
const val DB_STEP = -1

@SuppressLint("SetTextI18n")
class UiHandler(
    private val context: MainActivity,
) {
    private val application: MainApplication = MainApplication.getInstance()
    private val amplitudeTextView: TextView = context.findViewById(R.id.amplitudeText)
    private val dbShiftNum: EditText = context.findViewById(R.id.dbShiftNum)
    private val dbTargetNum: EditText = context.findViewById(R.id.dbTargetNum)
    private val audioFlowLayout: LinearLayout = context.findViewById((R.id.audioFlowLayout))
    private var maxAmpDbu: Float = -50f
    private var rmsAmpDbu: Float = -50f
    private var nSamples: Int = 0

    private val drawables: Map<String, List<Drawable>>
    private val dbThresholds: List<Float>
    private val optionsLayout: LinearLayout
    private val debugLayout: LinearLayout
    private var optionsLayoutVisible = false
    private var debugLayoutVisible = false
    var uiChart: UiChart

    private val buttonHeight: Int

    private val startWifiButton = context.findViewById<Button>(R.id.startWifi)
    private val toggleOptionsButton: Button = context.findViewById(R.id.toggleOptionsButton)
    private val toggleDebugButton: Button = context.findViewById(R.id.toggleDebugButton)
    private val deleteDataButton = context.findViewById<Button>(R.id.deleteDataButton)
    private val generateDataButton = context.findViewById<Button>(R.id.generateDataButton)
    private val startRecordButton = context.findViewById<Button>(R.id.startButton)


    init {
        dbThresholds = getDbThresholds()
        drawables = generateDrawables()
        initLeds()
        uiChart = UiChart(context.findViewById(R.id.lineChart), dbThresholds)
        debugLayout = context.findViewById(R.id.debugLayout)
        optionsLayout = context.findViewById(R.id.optionsLayout)
        applyVisibility()
        initOptionButtons()
        toggleOptionsButton.setOnClickListener {
            optionsLayoutVisible = !optionsLayoutVisible
            applyVisibility()
        }
        buttonHeight = toggleOptionsButton.height
        toggleDebugButton.setOnClickListener {
            debugLayoutVisible = !debugLayoutVisible
            applyVisibility()
        }
        deleteDataButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        generateDataButton.setOnClickListener {
            generateData()
        }

        startRecordButton.setOnClickListener {
            application.toggleRecording()
        }

        startWifiButton.setOnClickListener {
            application.toggleWifi()
        }
    }


    private fun initOptionButtons() {
        val timeSelector = context.findViewById<LinearLayout>(R.id.timeSelector)
        val dbShiftSelectorLayout = context.findViewById<LinearLayout>(R.id.dbShiftSelector)
        val dbTargetSelectorLayout = context.findViewById<LinearLayout>(R.id.dbTargetSelector)
        val editTextDbShift = dbShiftSelectorLayout.getChildAt(0)
        val editTextDbTarget = dbTargetSelectorLayout.getChildAt(0)
        val valuesTimeButtons =
            listOf<Long>(6 * 3600 * 1000, 3 * 3600 * 1000, 3600 * 1000, 1800 * 1000, 600 * 1000)
        val textTimeButtons = listOf<String>("6h", "3h", "1h", "30m", "10m")
        val valuesDbButtons = listOf<Float>(-1f, -0.1f, 0f, 0.1f, 1f)
        val textsDbButtons = listOf<String>("-1", "-0.1", "", "+0.1", "+1")
        dbShiftSelectorLayout.removeAllViews()
        dbTargetSelectorLayout.removeAllViews()

        // button layout
        val height = (Resources.getSystem().displayMetrics.density * 55).toInt()
        val layoutParams = LinearLayout.LayoutParams(0, height)
        layoutParams.weight = 1f

        for (i in 0 until 5) {
            // time frame
            var button = Button(context)
            button.text = textTimeButtons[i]
            // button.textSize = 10f
            button.layoutParams = layoutParams
            button.setOnClickListener {
                application.showMilliseconds = valuesTimeButtons[i]
                context.databaseHandler.renewDataQuery()
                updateText()
            }
            timeSelector.addView(button)

            // dbShift and dbTarget
            if (i == 2) {
                dbShiftSelectorLayout.addView(editTextDbShift)
                dbTargetSelectorLayout.addView((editTextDbTarget))
            } else {
                // shift selector
                button = Button(context)
                button.text = textsDbButtons[i]
                button.layoutParams = layoutParams
                button.setOnClickListener {
                    application.dbShift += valuesDbButtons[i]
                    updateText()
                }
                dbShiftSelectorLayout.addView(button)

                // target selector
                button = Button(context)
                button.text = textsDbButtons[i]
                button.layoutParams = layoutParams
                button.setOnClickListener {
                    application.dbTarget += valuesDbButtons[i]
                    updateText()
                }
                dbTargetSelectorLayout.addView(button)
            }
        }
        updateText()
        updateLeds()
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


    fun updateUI(data: Map<String, Int>) {
        if (data.containsKey("maxAmplitudeDbu")) {
            maxAmpDbu = (data["maxAmplitudeDbu"]!! + application.dbShift).round(1)
        }
        if (data.containsKey("rmsAmplitudeDbu")) {
            rmsAmpDbu = (data["rmsAmplitudeDbu"]!! + application.dbShift).round(1)
        }
        if (data.containsKey("nSamples")) {
            nSamples = data["nSamples"]!!
        }

        updateButtons()
        updateText()
        updateLeds()
    }

    fun updateButtons() {
        context.handler.post {
            if (application.recordingOn) {
                startRecordButton.text = "Stop Recording"
            } else {
                startRecordButton.text = "Start Recording"
            }

            if (application.wifiOn) {
                startWifiButton.text = "Stop Wifi"
            } else {
                startWifiButton.text = "Start Wifi"
            }
        }
    }

    private fun updateText() {
        context.handler.post {
            val outText =
                "max: $maxAmpDbu, rms: $rmsAmpDbu, nSamples: $nSamples, ${application.showMilliseconds}"
            amplitudeTextView.text = outText
            dbShiftNum.setText(application.dbShift.round(1).toString())
            dbTargetNum.setText(application.dbTarget.round(1).toString())
        }
    }

    private fun showDeleteConfirmationDialog() {
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
        val audioFlowLayout = context.findViewById<LinearLayout>(R.id.audioFlowLayout)
        val audioFlowLayoutText = context.findViewById<LinearLayout>(R.id.audioFlowLayoutText)
        val widthView = (Resources.getSystem().displayMetrics.density * 40).toInt()
        val widthText = (Resources.getSystem().displayMetrics.density * 40).toInt()
        val layoutParamsView = LinearLayout.LayoutParams(widthView, 0)
        layoutParamsView.weight = 1f
        val layoutParamsText = LinearLayout.LayoutParams(widthText, 0)
        layoutParamsText.weight = 1f

        for (i in 0 until N_LEDS) {
            val view = View(context)
            view.layoutParams = layoutParamsView
            view.setBackgroundColor((0xFF000000).toInt())
            audioFlowLayout.addView(view)

            if (i % 2 == 0) {
                val thresh = dbThresholds[N_LEDS - 1 - i]
                val text = TextView(context)
                text.layoutParams = layoutParamsText
                text.text = "$thresh"
                text.textSize = 11f
                text.gravity = Gravity.END
                text.setPadding(0, 0, 10, 0)
                audioFlowLayoutText.addView(text)
            }
        }
    }

    private fun getDbThresholds(): List<Float> {
        val dbThresholds = mutableListOf<Float>()
        var currentValue = DB_HIGH

        repeat(N_LEDS) {
            dbThresholds.add(currentValue.toFloat())
            currentValue += DB_STEP
        }
        return dbThresholds.reversed()
    }

    private fun updateLeds() {
        context.handler.post {
            for (index in 0 until N_LEDS) {
                val thresh = dbThresholds[index]
                val led = audioFlowLayout.getChildAt(N_LEDS - 1 - index) as View
                led.background = getDrawable(maxAmpDbu = maxAmpDbu, thresh = thresh)
            }
        }
    }

    private fun getDrawable(maxAmpDbu: Float, thresh: Float): Drawable {
        val redThresh = 12.0f
        val orangeThresh = 8.0f
        val ledOn = maxAmpDbu > thresh
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
            "off" to rgbColors.map { darkenColor(it) }.map { createDrawable(it) }
        )
    }

    private fun createDrawable(color: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        drawable.setStroke(4, Color.BLACK)
        return drawable
    }

    private fun darkenColor(colorInt: Int): Int {
        val factor = 0.3
        val red = (Color.red(colorInt) * factor).toInt()
        val green = (Color.green(colorInt) * factor).toInt()
        val blue = (Color.blue(colorInt) * factor).toInt()
        return Color.argb(255, red, green, blue)
    }

    private fun generateData() {
        val random = Random()
        repeat(30) {
            val time =
                System.currentTimeMillis() - random.nextInt(1000 * 3600 * 10)  // from last 10 hours
            val maxAmpDbu = random.nextFloat() * 25 - 10 - application.dbShift
            val rmsAmpDbu = 0.6f * (random.nextFloat() * 25 - 10) - application.dbShift
            context.databaseHandler.insertData(
                time = time,
                maxAmpDbu = maxAmpDbu,
                rmsAmpDbu = rmsAmpDbu
            )
        }
    }
}


fun Float.round(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10f }
    return (this * multiplier).roundToInt() / multiplier
}