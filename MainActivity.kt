package com.example.kassenbonpro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_RESULT_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Erstellt das WebView direkt im Code (spart die XML-Datei, falls gewünscht)
        webView = WebView(this)
        setContentView(webView)

        setupWebViewSettings()
        setupWebClients()

        // Lädt die index.html aus dem Ordner: app/src/main/assets/
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun setupWebViewSettings() {
        webView.settings.apply {
            // Wichtig für Puter.ai & Tesseract.js
            javaScriptEnabled = true 
            
            // Wichtig für das Speichern der Bons (localStorage)
            domStorageEnabled = true
            databaseEnabled = true
            
            // Ermöglicht Dateizugriff für die Bilder
            allowFileAccess = true
            allowContentAccess = true
            
            // Optimiert die Darstellung auf dem Handy
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
        }
        
        // Aktiviert Hardware-Beschleunigung für flüssiges OCR
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
    }

    private fun setupWebClients() {
        // Standard-Client für interne Navigation
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Alle Links direkt in der App öffnen
            }
        }

        // WebChromeClient steuert den Datei-Upload (Bilder auswählen)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Falls schon ein Prozess läuft, abbrechen
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*" // Nur Bilder anzeigen
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Mehrere Bons gleichzeitig
                }

                startActivityForResult(
                    Intent.createChooser(intent, "Kassenbon auswählen"),
                    FILE_CHOOSER_RESULT_CODE
                )
                return true
            }
        }
    }

    // Verarbeitet das ausgewählte Bild und sendet es zurück an das HTML-Feld
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) return
            
            val results: Array<Uri>? = if (resultCode == Activity.RESULT_OK) {
                if (data?.clipData != null) { // Mehrere Bilder
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                } else if (data?.data != null) { // Einzelnes Bild
                    arrayOf(data.data!!)
                } else null
            } else null

            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    // Ermöglicht das Zurückgehen innerhalb der App-Tabs mit dem Android-Back-Button
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
