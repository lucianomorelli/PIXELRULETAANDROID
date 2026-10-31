package com.pixelrosario.ruletainteractiva

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.JavascriptInterface
import java.io.BufferedReader
import java.io.InputStreamReader
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    /** Registro de demo en Descargas compartidas: sobrevive a la desinstalación normal
     * en Android 10+ (mientras el usuario no borre el archivo o restablezca el equipo).
     */
    inner class DemoStorageBridge {
        private fun recordName(): String {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(androidId.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            return "pixel_ruleta_demo_$digest.dat"
        }

        @JavascriptInterface
        fun getDemoSpins(): String {
            if (android.os.Build.VERSION.SDK_INT < 29) return "-1"
            return try {
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                contentResolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                    arrayOf(recordName(), "Download/PixelRuleta/"),
                    null
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) "0" else {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val uri = android.content.ContentUris.withAppendedId(collection, id)
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText().trim() } ?: "-1"
                    }
                } ?: "-1"
            } catch (_: Exception) { "-1" }
        }

        @JavascriptInterface
        fun setDemoSpins(value: String): Boolean {
            if (android.os.Build.VERSION.SDK_INT < 29) return false
            val count = value.toIntOrNull()?.coerceIn(0, 1000000000) ?: return false
            return try {
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
                val args = arrayOf(recordName(), "Download/PixelRuleta/")
                val existing = contentResolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, args, null)?.use { cursor ->
                    if (cursor.moveToFirst()) android.content.ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
                }
                val uri = existing ?: contentResolver.insert(collection, android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, recordName())
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/PixelRuleta/")
                }) ?: return false
                contentResolver.openOutputStream(uri, if (existing == null) "w" else "wt")?.use {
                    it.write(count.toString().toByteArray(Charsets.UTF_8))
                } ?: return false
                true
            } catch (_: Exception) { false }
        }
    }

    @Suppress("DEPRECATION")
    private fun enterKioskMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterKioskMode()

        webView = WebView(this)
        webView.addJavascriptInterface(DemoStorageBridge(), "PixelDemoStorage")
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = true
            setGeolocationEnabled(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                request?.url?.let { assetLoader.shouldInterceptRequest(it) }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.host == "appassets.androidplatform.net") return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    true
                }
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // La ruleta funciona como tótem/kiosco: el botón Atrás no cierra la aplicación.
            }
        })
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterKioskMode()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
