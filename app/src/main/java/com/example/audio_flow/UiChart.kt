package com.example.audio_flow

import android.annotation.SuppressLint
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Date

class UiChart(private val chart: LineChart, dbThresholds: List<Float>) {

    private val application: MainApplication = MainApplication.getInstance()

    init {
        chart.xAxis.valueFormatter = LineChartXAxisValueFormatter()
        chart.xAxis.enableGridDashedLine(10f, 10f, 0f)

        chart.isScaleXEnabled = false
        chart.isScaleYEnabled = false
        chart.setPinchZoom(false)

        chart.isDragEnabled = false
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerDragEnabled = false
        chart.isHighlightPerTapEnabled = false

        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawLabels(false)
        chart.axisRight.setDrawGridLines(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.axisLeft.axisMaximum = dbThresholds.last()
        chart.axisLeft.axisMinimum = dbThresholds.first()

        // layout
        chart.minOffset = 3f
        chart.extraTopOffset = 6f
        //chart.offsetLeftAndRight(0)
        //chart.setViewPortOffsets(3f, 50f, 3f, 10f)

        // dark mode
        chart.xAxis.textColor = Color.WHITE
        chart.axisLeft.textColor = Color.WHITE
        chart.setBackgroundColor(Color.BLACK)
    }

    fun updateChart(data: List<Value>) {
        if (chart.data != null &&
            chart.data.dataSetCount > 0
        ) { // update
            val setMaxAmplitude = chart.data.getDataSetByIndex(0) as LineDataSet
            setMaxAmplitude.values = data.map {
                Entry(
                    (it.time - TIME_SHIFT).toFloat(),
                    it.maxAmpDbu + application.dbShift
                )
            }
            val setRmsAmplitude = chart.data.getDataSetByIndex(1) as LineDataSet
            setRmsAmplitude.values = data.map {
                Entry(
                    (it.time - TIME_SHIFT).toFloat(),
                    it.rmsAmpDbu + application.dbShift
                )
            }
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        } else {
            // create datasets
            val maxDataSet =
                LineDataSet(data.map {
                    Entry(
                        (it.time - TIME_SHIFT).toFloat(),
                        it.maxAmpDbu + application.dbShift
                    )
                }, "max amplitude")
            maxDataSet.setDrawCircles(false)
            maxDataSet.setDrawValues(false)
            maxDataSet.color = 0xFFFF0000.toInt()
            val rmsDataSet = LineDataSet(data.map {
                Entry(
                    (it.time - TIME_SHIFT).toFloat(),
                    it.rmsAmpDbu + application.dbShift
                )
            }, "rms amplitude")
            rmsDataSet.setDrawCircles(false)
            rmsDataSet.setDrawValues(false)
            rmsDataSet.color = 0xFF00FF00.toInt()
            val lineData = LineData(maxDataSet, rmsDataSet)

            chart.data = lineData
            chart.invalidate()
        }
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