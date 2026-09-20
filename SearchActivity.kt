package com.ahmed.aiagent

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.aiagent.databinding.ActivitySearchBinding
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {

    private lateinit var b: ActivitySearchBinding

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                b.editQuery.setText(text)
                search(text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.webView.settings.javaScriptEnabled = true
        b.webView.settings.domStorageEnabled = true

        b.btnSearch.setOnClickListener {
            val q = b.editQuery.text.toString().trim()
            if (q.isNotEmpty()) search(q)
            else Toast.makeText(this, "Kuch likho ya bolo", Toast.LENGTH_SHORT).show()
        }

        b.btnMic.setOnClickListener {
            try {
                voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Kya search karna hai…")
                })
            } catch (e: Exception) {
                Toast.makeText(this, "Voice available nahi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun search(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        b.webView.loadUrl("https://lite.duckduckgo.com/lite/?q=$encoded")
    }
}
