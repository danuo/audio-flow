package com.example.audio_flow

import android.annotation.SuppressLint
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
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

    fun updateLimit(limit: Float, invalidate: Boolean) {
        chart.axisLeft.removeAllLimitLines()

        // add a LimitLine
        var limitLine = LimitLine(limit, "dB target")
        limitLine.lineColor = 0xFF0000FF.toInt()
        limitLine.lineWidth = 2f
        chart.axisLeft.addLimitLine(limitLine)

        if (invalidate) {
            chart.invalidate()
        }
    }

    private fun clearChart() {
        chart.data = null
        chart.axisLeft.removeAllLimitLines()
        chart.invalidate()
    }

    private fun createEntryList(xData: List<Float>, yData: List<Float>): List<Entry> {
        assert(xData.size == yData.size)
        return xData.zip(yData).map {
            Entry(it.first, it.second)
        }
    }

    private fun getTimeList(data: List<Value>): List<Float> {
        return data.map {
            (it.time - TIME_SHIFT).toFloat()
        }
    }

    private fun getMaxAmpDbu(data: List<Value>): List<Float> {
        return data.map {
            it.maxAmpDbu + application.dbShift
        }
    }

    private fun getRmsAmpDbu(data: List<Value>): List<Float> {
        return data.map {
            it.rmsAmpDbu + application.dbShift
        }
    }

    fun updateChart(data: List<Value>) {
        if (data.isEmpty()) {
            clearChart()
            return
        }

        if (chart.data != null &&
            chart.data.dataSetCount > 0
        ) { // update
            val setMaxAmplitude = chart.data.getDataSetByIndex(0) as LineDataSet
            setMaxAmplitude.values =
                createEntryList(getTimeList(data), getMaxAmpDbu(data))
            val setRmsAmplitude = chart.data.getDataSetByIndex(1) as LineDataSet
            setRmsAmplitude.values =
                createEntryList(getTimeList(data), getRmsAmpDbu(data))
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        } else {
            // create datasets
            val maxDataSet =
                LineDataSet(
                    createEntryList(
                        getTimeList(data),
                        getMaxAmpDbu(data)
                    ), "max amplitude"
                )
            maxDataSet.setDrawCircles(false)
            maxDataSet.setDrawValues(false)
            maxDataSet.color = 0xFFFF0000.toInt()
            val rmsDataSet =
                LineDataSet(
                    createEntryList(getTimeList(data), getRmsAmpDbu(data)),
                    "rms amplitude"
                )
            rmsDataSet.setDrawCircles(false)
            rmsDataSet.setDrawValues(false)
            rmsDataSet.color = 0xFF00FF00.toInt()
            val lineData = LineData(maxDataSet, rmsDataSet)

            chart.data = lineData

            updateLimit(application.dbTarget, invalidate = false)

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