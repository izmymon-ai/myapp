package com.ahmed.aiagent

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.aiagent.databinding.ActivityImageGenBinding

class ImageGenActivity : AppCompatActivity() {

    private lateinit var b: ActivityImageGenBinding
    private var lastBitmap: Bitmap? = null

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                b.editPrompt.setText(text)
                generate(text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityImageGenBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnGenerate.setOnClickListener {
            val prompt = b.editPrompt.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Kuch likho ya bolo pehle", Toast.LENGTH_SHORT).show()
            } else {
                generate(prompt)
            }
        }

        b.btnMic.setOnClickListener {
            try {
                voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe karo kya image chahiye...")
                })
            } catch (e: Exception) {
                Toast.makeText(this, "Voice is device par available nahi", Toast.LENGTH_SHORT).show()
            }
        }

        b.btnSave.setOnClickListener {
            val bmp = lastBitmap ?: return@setOnClickListener
            val ok = ImageSaveHelper.save(this, bmp, "Generated")
            Toast.makeText(this,
                if (ok) "✅ Gallery mein save ho gayi!" else "❌ Save nahi hua",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun generate(prompt: String) {
        b.progressBar.visibility = View.VISIBLE
        b.btnGenerate.isEnabled  = false
        b.btnMic.isEnabled       = false
        b.txtStatus.text         = "Image ban rahi hai… (30–60 sec)"
        b.imageResult.visibility = View.GONE
        b.btnSave.visibility     = View.GONE

        PollinationsApi.generateImage(
            prompt,
            onResult = { bmp ->
                runOnUiThread {
                    b.progressBar.visibility = View.GONE
                    b.btnGenerate.isEnabled  = true
                    b.btnMic.isEnabled       = true
                    if (bmp != null) {
                        lastBitmap = bmp
                        b.imageResult.setImageBitmap(bmp)
                        b.imageResult.visibility = View.VISIBLE
                        b.btnSave.visibility     = View.VISIBLE
                        b.txtStatus.text         = "✅ Image tayyar! Save karo ya naya prompt likho."
                    } else {
                        b.txtStatus.text = "❌ Image nahi bani — internet check karo"
                    }
                }
            },
            onError = { err ->
                runOnUiThread {
                    b.progressBar.visibility = View.GONE
                    b.btnGenerate.isEnabled  = true
                    b.btnMic.isEnabled       = true
                    b.txtStatus.text         = "❌ $err"
                }
            }
        )
    }
}
