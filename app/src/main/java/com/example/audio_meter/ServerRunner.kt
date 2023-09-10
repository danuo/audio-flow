package com.example.audio_meter

import android.util.Log
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking


class ServerRunner(private val htmlString: String) : Runnable {

    private var repository: ValueRepository? = null

    private val gson = Gson()

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
            Log.d("servernew", "this worked here, database is not null")
        }
        embeddedServer(Netty, port = 4444) {
            extracted()
        }.start(wait = true)
        Log.d("servernew", "do we ever reach here")
    }

    private fun getDataFromDatabase(): Map<String, List<Any>> {
        // val timeStamp = initTime - context.showMilliseconds
        val timeStamp = System.currentTimeMillis() - 1000 * 60 * 30  // 30 min
        var dataList = listOf<Value>()
        return runBlocking {
            if (repository is ValueRepository) {
                Log.d("servernew", "this worked here, repository is not null")
                dataList = repository!!.getValuesAll()
                Log.d("servernew", dataList.size.toString())
                Log.d("servernew", dataList.toString())
            }

            return@runBlocking mapOf<String, List<Any>>("time" to dataList.map { it.time },
                "values" to dataList.map { it.value })
            //"values" to data.map { it.value + context.dbShift })
        }
    }
}