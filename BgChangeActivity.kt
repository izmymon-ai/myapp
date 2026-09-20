package com.ahmed.agentapp

import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.agentapp.databinding.ActivityBgChangeBinding

class BgChangeActivity : AppCompatActivity() {

    private lateinit var b: ActivityBgChangeBinding
    private var originalBitmap: Bitmap? = null
    private var fgBitmap: Bitmap? = null   // foreground (background removed)
    private var resultBitmap: Bitmap? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = contentResolver.openInputStream(it)
            originalBitmap = BitmapFactory.decodeStream(stream)
            b.imageOriginal.setImageBitmap(originalBitmap)
            b.imageOriginal.visibility = View.VISIBLE
            b.btnRemoveBg.isEnabled = true
            b.txtStatus.text = "Image load ho gayi — background hatao"
        }
    }

    private val pickBgImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = contentResolver.openInputStream(it)
            val bgBitmap = BitmapFactory.decodeStream(stream) ?: return@let
            applyImageBackground(bgBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBgChangeBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        b.btnRemoveBg.setOnClickListener {
            val bmp = originalBitmap ?: return@setOnClickListener
            b.progressBar.visibility = View.VISIBLE
            b.txtStatus.text = "Background hata raha hai..."

            HuggingFaceApi.removeBackground(bmp,
                onResult = { result ->
                    runOnUiThread {
                        b.progressBar.visibility = View.GONE
                        if (result != null) {
                            fgBitmap = result
                            b.imageResult.setImageBitmap(result)
                            b.imageResult.visibility = View.VISIBLE
                            b.bgOptions.visibility = View.VISIBLE
                            b.txtStatus.text = "✅ Background hat gaya! Ab naya background choose karo."
                        } else {
                            b.txtStatus.text = "❌ Nahi hua — token check karo"
                        }
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        b.progressBar.visibility = View.GONE
                        b.txtStatus.text = "❌ $err"
                    }
                }
            )
        }

        // Solid color backgrounds
        b.btnBgWhite.setOnClickListener  { applySolidBackground(Color.WHITE) }
        b.btnBgBlack.setOnClickListener  { applySolidBackground(Color.BLACK) }
        b.btnBgBlue.setOnClickListener   { applySolidBackground(Color.parseColor("#1565C0")) }
        b.btnBgGreen.setOnClickListener  { applySolidBackground(Color.parseColor("#2E7D32")) }
        b.btnBgRed.setOnClickListener    { applySolidBackground(Color.parseColor("#C62828")) }
        b.btnBgYellow.setOnClickListener { applySolidBackground(Color.parseColor("#F9A825")) }

        // Custom image background from gallery
        b.btnBgCustom.setOnClickListener { pickBgImage.launch("image/*") }

        // AI generated background
        b.btnBgAi.setOnClickListener {
            val prompt = b.editBgPrompt.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Background ka description likho", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            b.txtStatus.text = "AI background bana raha hai..."
            b.progressBar.visibility = View.VISIBLE
            HuggingFaceApi.generateImage(prompt,
                onResult = { bgBitmap ->
                    runOnUiThread {
                        b.progressBar.visibility = View.GONE
                        bgBitmap?.let { applyImageBackground(it) }
                            ?: run { b.txtStatus.text = "❌ Background nahi bana" }
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        b.progressBar.visibility = View.GONE
                        b.txtStatus.text = "❌ $err"
                    }
                }
            )
        }

        b.btnSave.setOnClickListener { saveResult() }
    }

    private fun applySolidBackground(color: Int) {
        val fg = fgBitmap ?: return
        val w = fg.width; val h = fg.height
        val canvas = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(canvas)
        c.drawColor(color)
        c.drawBitmap(fg, 0f, 0f, null)
        resultBitmap = canvas
        b.imageResult.setImageBitmap(canvas)
        b.btnSave.visibility = View.VISIBLE
        b.txtStatus.text = "✅ Background badal gaya! Save karo."
    }

    private fun applyImageBackground(bgBitmap: Bitmap) {
        val fg = fgBitmap ?: return
        val w = fg.width; val h = fg.height
        val scaledBg = Bitmap.createScaledBitmap(bgBitmap, w, h, true)
        val canvas = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(canvas)
        c.drawBitmap(scaledBg, 0f, 0f, null)
        c.drawBitmap(fg, 0f, 0f, null)
        resultBitmap = canvas
        b.imageResult.setImageBitmap(canvas)
        b.btnSave.visibility = View.VISIBLE
        b.txtStatus.text = "✅ Background badal gaya! Save karo."
    }

    private fun saveResult() {
        val bmp = resultBitmap ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "BgChanged_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AgentApp")
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, "✅ Gallery mein save ho gayi!", Toast.LENGTH_SHORT).show()
        }
    }
}
