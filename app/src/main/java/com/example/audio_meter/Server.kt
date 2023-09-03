package com.example.audio_meter

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context

class Server(contextRoot: Context, private val databaseHandler: DatabaseHandler) {
    private val gson = Gson()
    private val htmlResourceId = R.raw.index
    private val htmlString =
        loadHtmlResourceToString(context = contextRoot, htmlResourceId).trimIndent()

    init {
        startServer()
    }

    fun startServer() {
        println("Starting CustomServer")
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
        val data = databaseHandler.newestData
        return mapOf<String, List<Any>>("time" to data.map { it.time },
            "values" to data.map { it.value })
    }

    private fun loadHtmlResourceToString(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        return inputStream.readBytes().toString(Charsets.UTF_8)
    }
}
