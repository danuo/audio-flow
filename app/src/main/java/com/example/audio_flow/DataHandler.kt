package com.example.audio_flow

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

    init {
        cleanupDatabase()
        context.lifecycleScope.launch {
            viewModel.getDataCount().collect() { dataCount ->
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
        viewModel.deleteAll()
    }

    private fun cleanupDatabase() {
        // delete data that is older than 10 days
        val time: Long = System.currentTimeMillis() - 10 * 24 * 3600 * 1000
        viewModel.deleteOlderThan(time)
    }
}