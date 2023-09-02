package com.example.audio_meter

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class Server(private val databaseHandler: DatabaseHandler) {
    private val temperatureDataTemp = listOf(
        Pair(System.currentTimeMillis() - 600000, 22.5), // 10 minutes ago
        Pair(System.currentTimeMillis() - 540000, 23.0), // 9.5 minutes ago
        Pair(System.currentTimeMillis() - 480000, 22.8), // 8 minutes ago
        Pair(System.currentTimeMillis() - 420000, 23.2), // 7 minutes ago
        Pair(System.currentTimeMillis() - 360000, 23.5), // 6 minutes ago
        Pair(System.currentTimeMillis() - 300000, 23.8), // 5 minutes ago
        Pair(System.currentTimeMillis() - 240000, 24.0), // 4 minutes ago
        Pair(System.currentTimeMillis() - 180000, 24.2), // 3 minutes ago
        Pair(System.currentTimeMillis() - 120000, 24.5), // 2 minutes ago
        Pair(System.currentTimeMillis() - 60000, 24.8)  // 1 minute ago
    )
    val gson = Gson()

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
                call.respondText(generateHtml(temperatureDataTemp), ContentType.Text.Html)
            }

            get("/data") {
                var data = getDataDummy()
                data = getDataFromDatabase()
                call.respondText(
                    gson.toJson(data)
                )
            }
        }
    }

    private fun getDataDummy(): Map<String, List<Any>> {
        return mapOf("time" to temperatureDataTemp.map { it.first },
            "values" to temperatureDataTemp.map { it.second })
    }

    private fun getDataFromDatabase(): Map<String, List<Any>> {
        val data = databaseHandler.newestData

        println("test")
        return mapOf<String, List<Any>>("time" to data.map { it.time },
            "values" to data.map { it.value })
    }

    fun generateHtml(temperatureData: List<Pair<Long, Double>>): String {
        val timeLabels = temperatureData.map { it.first.toString() }
        val temperatureValues = temperatureData.map { it.second }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>State Display</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/moment@^2"></script>
            <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment@^1"></script>
        </head>
        <body>
            <h1>Temperature Over the Last 10 Minutes</h1>
            <div style="width: 80%;">
                <canvas id="temperatureChart"></canvas>
            </div>
            <form action="/update" method="get">
                <label for="newValue">New Value:</label>
                <input type="text" id="newValue" name="newValue">
                <input type="submit" value="Update">
            </form>
            <script>
                async function fetchTemperatureData() {
                    try {
                        const response = await fetch('/data');
                        if (!response.ok) {
                            throw new Error('Failed to fetch data');
                        }
                        return await response.json();
                    } catch (error) {
                        console.error('Error fetching data:', error);
                        return { time: [], values: [] };
                    }
                }
                
                async function updateChart(chart) {
                    const data = await fetchTemperatureData();

                    // Update the chart with the fetched data
                    chart.data.labels = data.time;
                    chart.data.datasets[0].data = data.values;
                    chart.update();
                }

                var ctx = document.getElementById('temperatureChart');

                var chart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: [],
                        datasets: [{
                            label: 'Temperature (°C)',
                            data: [],
                            borderColor: 'blue',
                            fill: false
                        }]
                    },
                    options: {
                        scales: {
                            x: {
                                type: 'time',
                                time: {
                                    unit: 'minute'
                                },
                                title: {
                                    display: true,
                                    text: 'Time'
                                }
                            },
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Temperature (°C)'
                                }
                            }
                        }
                    }
                });
                updateChart(chart);

                // Auto-refresh the chart every 60 seconds (or as needed)
                setInterval(function() {
                    updateChart(chart);
                }, 1000 * 5); // every 5 sek
            </script>
        </body>
        </html>
    """.trimIndent()
    }

}
