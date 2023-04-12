package com.wm.astroplay.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import com.wm.astroplay.databinding.ActivityTermsBinding

class TermsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTermsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webView.webViewClient = WebViewClient()
        binding.webView.settings.javaScriptEnabled = true

        val pdfUrl = "https://drive.google.com/file/d/1Rz7gOmUYx2lfRE_Toe9PxmWpAbjNiqqH/view"

        binding.webView.loadUrl("https://drive.google.com/file/d/1Rz7gOmUYx2lfRE_Toe9PxmWpAbjNiqqH/view")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}