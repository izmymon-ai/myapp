package com.ahmed.agentapp

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.View
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.agentapp.databinding.ActivityImageGenBinding

class ImageGenActivity : AppCompatActivity() {

    private lateinit var b: ActivityImageGenBinding
    private var lastBitmap: Bitmap? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                b.editPrompt.setText(spoken)
                generateImage(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityImageGenBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnGenerate.setOnClickListener {
            val prompt = b.editPrompt.text.toString().trim()
            if (prompt.isNotEmpty()) generateImage(prompt)
            else Toast.makeText(this, "Kuch likho ya bolo pehle", Toast.LENGTH_SHORT).show()
        }

        b.btnMic.setOnClickListener {
            speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe karo — kya image banani hai...")
            })
        }

        b.btnSave.setOnClickListener { saveImage() }
    }

    private fun generateImage(prompt: String) {
        b.progressBar.visibility = View.VISIBLE
        b.btnGenerate.isEnabled = false
        b.txtStatus.text = "AI image bana raha hai... (30-60 sec)"
        b.btnSave.visibility = View.GONE

        HuggingFaceApi.generateImage(prompt,
            onResult = { bitmap ->
                runOnUiThread {
                    b.progressBar.visibility = View.GONE
                    b.btnGenerate.isEnabled = true
                    if (bitmap != null) {
                        lastBitmap = bitmap
                        b.imageResult.setImageBitmap(bitmap)
                        b.imageResult.visibility = View.VISIBLE
                        b.btnSave.visibility = View.VISIBLE
                        b.txtStatus.text = "✅ Image tayyar! Save karo ya naya banao."
                    } else {
                        b.txtStatus.text = "❌ Image nahi bani — token check karo"
                    }
                }
            },
            onError = { err ->
                runOnUiThread {
                    b.progressBar.visibility = View.GONE
                    b.btnGenerate.isEnabled = true
                    b.txtStatus.text = "❌ Error: $err"
                }
            }
        )
    }

    private fun saveImage() {
        val bmp = lastBitmap ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "AI_Image_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AgentApp")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            Toast.makeText(this, "✅ Gallery mein save ho gayi!", Toast.LENGTH_SHORT).show()
        }
    }
}
