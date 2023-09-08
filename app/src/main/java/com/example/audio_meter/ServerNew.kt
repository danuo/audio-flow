package com.example.audio_meter

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking


class ServerNew() {
    private val database = ValueDatabase.getDatabase()
    private val repository = ValueRepository(database!!.valueDao())

    private val gson = Gson()

    var htmlString = ""
    
    fun startServer() {
        embeddedServer(Netty, port = 4444) {
            extracted()
        }.start(wait = true)
    }

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

    private fun getDataFromDatabase(): Map<String, List<Any>> {
        // val timeStamp = initTime - context.showMilliseconds
        val timeStamp = System.currentTimeMillis() - 1000 * 60 * 30  // 30 min
        return runBlocking {
            val dataList = repository.getValuesNewerThan(timeStamp).toList().first()

            return@runBlocking mapOf<String, List<Any>>("time" to dataList.map { it.time },
                "values" to dataList.map { it.value })
            //"values" to data.map { it.value + context.dbShift })
        }
    }
}