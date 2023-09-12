package com.example.audio_meter

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import android.content.Context

class ServerOld(private val context: MainActivity, private val databaseHandler: DatabaseHandler) {
    private val application: MainApplication = MainApplication.getInstance()
    private val gson = Gson()
    private val htmlResourceId = R.raw.index
    private val htmlString =
        loadHtmlResourceToString(context = context, htmlResourceId).trimIndent()

    init {
        startServer()
    }

    private fun startServer() {
        embeddedServer(Jetty, port = 4444) {
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
            "values" to data.map { it.maxAmpDbu + application.dbShift })
    }

    private fun loadHtmlResourceToString(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        return inputStream.readBytes().toString(Charsets.UTF_8)
    }
}
