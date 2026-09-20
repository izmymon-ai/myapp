package com.ahmed.agentapp

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.agentapp.databinding.ActivitySearchBinding
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {

    private lateinit var b: ActivitySearchBinding

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                b.editQuery.setText(spoken)
                doSearch(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.webResults.settings.javaScriptEnabled = true

        b.btnSearch.setOnClickListener {
            val q = b.editQuery.text.toString().trim()
            if (q.isNotEmpty()) doSearch(q)
            else Toast.makeText(this, "Kuch likho ya bolo", Toast.LENGTH_SHORT).show()
        }

        b.btnMic.setOnClickListener {
            speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Kya search karna hai...")
            })
        }
    }

    private fun doSearch(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        b.webResults.loadUrl("https://lite.duckduckgo.com/lite/?q=$encoded")
    }
}
