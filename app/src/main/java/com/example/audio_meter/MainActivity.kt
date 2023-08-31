package com.example.audio_meter

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView!!.settings.javaScriptEnabled = true
        webView!!.loadUrl("https://www.example.com")

        // Set up WebViewClient to handle page navigation within the WebView
        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        webView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView!!.onResume()
    }
}
