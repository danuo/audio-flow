package com.example.audio_meter

import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var textView: TextView
    private var counter = 0
    private val handler = Handler()
    private val updateRunnable = object : Runnable {
        override fun run() {
            textView.text = counter.toString()
            counter++
            handler.postDelayed(this, 1000) // Run again after 1 second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // Set the layout XML

        webView = findViewById(R.id.webView)
        webView!!.settings.javaScriptEnabled = true
        webView!!.loadUrl("https://www.example.com")

        textView = findViewById(R.id.textView)


        // Set up WebViewClient to handle page navigation within the WebView
        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }

        // Start updating the text every second
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        webView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView!!.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the updateRunnable callbacks to prevent memory leaks
        handler.removeCallbacks(updateRunnable)
    }
}
