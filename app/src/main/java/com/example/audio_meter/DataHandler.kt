package com.example.audio_meter

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DataHandler(
    private val context: MainActivity,
    private val uiHandler: UiHandler,
) {
    private val application: MainApplication = MainApplication.getInstance()
    private val viewModel =
        ViewModelProvider(context).get(modelClass = ValueViewModel::class.java)

    private var job: Job? = null
    var dataCount: Int = 10
    var newestData = listOf<Value>()

    init {
        cleanupDatabase()
        context.lifecycleScope.launch {
            viewModel.getDataCount().collect() { data ->
                dataCount = data
                uiHandler.updateUI(mapOf("nSamples" to dataCount))
            }
        }
        renewDataQuery()
    }

    fun renewDataQuery() {
        val initTime = System.currentTimeMillis()
        val timeStamp = initTime - application.showMilliseconds
        job?.cancel()
        job = context.lifecycleScope.launch {
            viewModel.getValuesNewerThan(timeStamp).collect() { data ->
                newestData = data
                if (data.isNotEmpty()) {
                    uiHandler.uiChart.updateChart(data)
                }
                if (System.currentTimeMillis() - initTime > 1000 * 10) {
                    renewDataQuery()
                }
            }
        }
    }

    fun insertData(time: Long, maxAmpDbu: Float, rmsAmpDbu: Float) {
        viewModel.insert(Value(time = time, maxAmpDbu = maxAmpDbu, rmsAmpDbu = rmsAmpDbu))
    }

    fun deleteAll() {
        newestData = listOf()
        viewModel.deleteAll()
    }

    private fun cleanupDatabase() {
        // delete data that is older than 10 days
        val time: Long = System.currentTimeMillis() - 10 * 24 * 3600 * 1000
        viewModel.deleteOlderThan(time)
    }
}