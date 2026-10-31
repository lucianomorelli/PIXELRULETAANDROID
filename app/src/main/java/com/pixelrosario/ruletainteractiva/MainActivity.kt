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

    /**
     * Registro compartido de demo. Se guarda en Download/PixelRuleta para sobrevivir
     * a la desinstalación. El acceso especial de Android 11+ se solicita al usuario.
     * Las lecturas/escrituras se ejecutan en un hilo de trabajo, nunca en el hilo UI.
     */
    private val demoIo = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var pendingPermissionCallback: String? = null

    private fun hasDemoStorageAccess(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.os.Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    inner class DemoStorageBridge {
        private fun recordFile(): java.io.File {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                ?: throw IllegalStateException("No se pudo obtener el identificador Android")
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(androidId.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val folder = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "PixelRuleta"
            )
            if (!folder.exists() && !folder.mkdirs()) throw IllegalStateException("No se pudo crear la carpeta de registro")
            return java.io.File(folder, "pixel_ruleta_demo_$digest.dat")
        }

        private fun readCount(): String {
            if (!hasDemoStorageAccess()) return "-1"
            return try {
                val file = recordFile()
                if (!file.exists()) "0" else file.readText(Charsets.UTF_8).trim().toIntOrNull()
                    ?.coerceIn(0, 1000000000)?.toString() ?: "-1"
            } catch (_: Exception) { "-1" }
        }

        private fun writeCount(value: String): Boolean {
            if (!hasDemoStorageAccess()) return false
            val count = value.toIntOrNull()?.coerceIn(0, 1000000000) ?: return false
            return try {
                val file = recordFile()
                val temp = java.io.File(file.parentFile, file.name + ".tmp")
                temp.writeText(count.toString(), Charsets.UTF_8)
                if (file.exists() && !file.delete()) return false
                temp.renameTo(file)
            } catch (_: Exception) { false }
        }

        private fun deliver(callbackId: String, value: String) {
            runOnUiThread {
                if (::webView.isInitialized) {
                    webView.evaluateJavascript(
                        "window.__pixelDemoStorageResult(${org.json.JSONObject.quote(callbackId)}, ${org.json.JSONObject.quote(value)})",
                        null
                    )
                }
            }
        }

        @JavascriptInterface
        fun getDemoSpins(callbackId: String) {
            demoIo.execute { deliver(callbackId, readCount()) }
        }

        @JavascriptInterface
        fun setDemoSpins(value: String, callbackId: String) {
            demoIo.execute { deliver(callbackId, writeCount(value).toString()) }
        }

        @JavascriptInterface
        fun requestDemoStorageAccess(callbackId: String) {
            runOnUiThread {
                if (hasDemoStorageAccess()) {
                    deliver(callbackId, "true")
                    return@runOnUiThread
                }
                pendingPermissionCallback = callbackId
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    } catch (_: Exception) {
                        pendingPermissionCallback = null
                        deliver(callbackId, "false")
                    }
                } else {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        7401
                    )
                }
            }
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

    override fun onResume() {
        super.onResume()
        val callback = pendingPermissionCallback
        if (callback != null) {
            pendingPermissionCallback = null
            val granted = hasDemoStorageAccess()
            if (::webView.isInitialized) {
                webView.postDelayed({
                    webView.evaluateJavascript(
                        "window.__pixelDemoStorageResult(${org.json.JSONObject.quote(callback)}, ${org.json.JSONObject.quote(granted.toString())})",
                        null
                    )
                }, 250)
            }
        }
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
