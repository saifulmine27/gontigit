package com.ghonti.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val rcGoogle = 9001

    companion object {
        @Volatile
        var foreground = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webChromeClient = WebChromeClient()

        // https://appassets.androidplatform.net/ থেকে লোড হয়, যাতে Firebase Auth ঠিকমতো কাজ করে
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        web.evaluateJavascript("if(window.nativeFired)nativeFired();if(window.pullSync)pullSync()", null)
    }

    override fun onPause() {
        foreground = false
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != rcGoogle) return
        try {
            val acc = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val tok = acc.idToken ?: ""
            web.evaluateJavascript("onGoogleToken('$tok')", null)
        } catch (e: ApiException) {
            web.evaluateJavascript("onGoogleError('${e.statusCode}')", null)
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun sync(json: String) {
            AlarmScheduler.saveAndSchedule(applicationContext, json)
        }

        @JavascriptInterface
        fun takeFired(): String = AlarmScheduler.takeFired(applicationContext)

        @JavascriptInterface
        fun googleSignIn() {
            runOnUiThread {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(this@MainActivity, gso)
                client.signOut().addOnCompleteListener {
                    @Suppress("DEPRECATION")
                    startActivityForResult(client.signInIntent, rcGoogle)
                }
            }
        }

        @JavascriptInterface
        fun setBars(css: String) {
            val n = Regex("\\d+").findAll(css).map { it.value.toInt() }.toList()
            if (n.size < 3) return
            runOnUiThread {
                val c = Color.rgb(n[0], n[1], n[2])
                window.statusBarColor = c
                window.navigationBarColor = c
                val light = (0.299 * n[0] + 0.587 * n[1] + 0.114 * n[2]) > 150
                var mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= 26) mask = mask or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                val f = window.decorView.systemUiVisibility
                window.decorView.systemUiVisibility = if (light) f or mask else f and mask.inv()
            }
        }
    }
}
