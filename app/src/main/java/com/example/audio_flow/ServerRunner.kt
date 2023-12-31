package com.example.audio_flow

import android.util.Log
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class ServerRunner(private val htmlString: String) : Runnable {
    private val application: MainApplication = MainApplication.getInstance()
    private val gson = Gson()
    private var repository: ValueRepository? = null
    private var server: JettyApplicationEngine? = null

    private fun Application.extracted() {
        routing {
            get("/") {
                call.respondText(htmlString, ContentType.Text.Html)
            }

            get("/data") {
                val data = getDataFromDatabase()
                call.respondText(
                    gson.toJson(data)
                )
            }
        }
    }

    override fun run() {
        val database = ValueDatabase.getDatabase()
        if (database is ValueDatabase) {
            repository = ValueRepository(database.valueDao())
            Log.d("ServerRunner", "this worked here, database is not null")
        }
        server = embeddedServer(Jetty, port = 4444) {
            extracted()
        }
        server?.start(wait = true)
        Log.d("ServerRunner", "end reached, Thread will stop")
    }

    fun stopServer() {
        server?.stop(100, 100)
    }

    private fun getDataFromDatabase(): Map<String, List<Any>> {
        val timeStamp = System.currentTimeMillis() - application.showMilliseconds
        var dataList = listOf<Value>()
        return runBlocking {
            repository?.let {
                dataList = repository!!.getValuesNewerThan(timeStamp).first()
            }
            return@runBlocking mapOf<String, List<Any>>("time" to dataList.map { it.time },
                "values" to dataList.map { it.maxAmpDbu + application.dbShift })
        }
    }
}