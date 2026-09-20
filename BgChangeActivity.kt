package com.ahmed.aiagent

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.aiagent.databinding.ActivityBgChangeBinding

class BgChangeActivity : AppCompatActivity() {

    private lateinit var b: ActivityBgChangeBinding
    private var fgBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = contentResolver.openInputStream(it)
                ?.let { s -> BitmapFactory.decodeStream(s) } ?: return@let
            processImage(bmp)
        }
    }

    private val pickBgImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = contentResolver.openInputStream(it)
                ?.let { s -> BitmapFactory.decodeStream(s) } ?: return@let
            applyBgImage(bmp)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBgChangeBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        b.btnWhite.setOnClickListener  { applyColor(Color.WHITE) }
        b.btnBlack.setOnClickListener  { applyColor(Color.BLACK) }
        b.btnBlue.setOnClickListener   { applyColor(Color.parseColor("#1565C0")) }
        b.btnGreen.setOnClickListener  { applyColor(Color.parseColor("#2E7D32")) }
        b.btnRed.setOnClickListener    { applyColor(Color.parseColor("#B71C1C")) }
        b.btnYellow.setOnClickListener { applyColor(Color.parseColor("#F57F17")) }
        b.btnGallery.setOnClickListener { pickBgImage.launch("image/*") }

        b.btnAiBg.setOnClickListener {
            val prompt = b.editBgPrompt.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Background ka description likho", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            b.txtStatus.text = "AI background bana raha hai…"
            b.progressBar.visibility = View.VISIBLE
            PollinationsApi.generateImage(prompt,
                onResult = { bmp -> runOnUiThread {
                    b.progressBar.visibility = View.GONE
                    bmp?.let { applyBgImage(it) } ?: run { b.txtStatus.text = "❌ Nahi bana" }
                }},
                onError = { err -> runOnUiThread {
                    b.progressBar.visibility = View.GONE
                    b.txtStatus.text = "❌ $err"
                }}
            )
        }

        b.btnSave.setOnClickListener {
            val bmp = resultBitmap ?: return@setOnClickListener
            val ok = ImageSaveHelper.save(this, bmp, "BgChanged")
            Toast.makeText(this,
                if (ok) "✅ Gallery mein save ho gayi!" else "❌ Save nahi hua",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImage(bmp: Bitmap) {
        // Simple background removal: makes near-white pixels transparent
        val w = bmp.width; val h = bmp.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val bl = Color.blue(pixels[i])
            // Keep pixel if it's dark enough (not background)
            pixels[i] = if (r > 230 && g > 230 && bl > 230) Color.TRANSPARENT else pixels[i]
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        fgBitmap = out
        b.imageResult.setImageBitmap(out)
        b.imageResult.visibility = View.VISIBLE
        b.bgOptions.visibility   = View.VISIBLE
        b.txtStatus.text = "✅ Background hat gaya! Ab naya background choose karo."
    }

    private fun applyColor(color: Int) {
        val fg = fgBitmap ?: return
        val canvas = Bitmap.createBitmap(fg.width, fg.height, Bitmap.Config.ARGB_8888)
        Canvas(canvas).apply {
            drawColor(color)
            drawBitmap(fg, 0f, 0f, null)
        }
        resultBitmap = canvas
        b.imageResult.setImageBitmap(canvas)
        b.btnSave.visibility = View.VISIBLE
        b.txtStatus.text = "✅ Background badal gaya — save karo!"
    }

    private fun applyBgImage(bgBmp: Bitmap) {
        val fg = fgBitmap ?: return
        val scaled = Bitmap.createScaledBitmap(bgBmp, fg.width, fg.height, true)
        val canvas = Bitmap.createBitmap(fg.width, fg.height, Bitmap.Config.ARGB_8888)
        Canvas(canvas).apply {
            drawBitmap(scaled, 0f, 0f, null)
            drawBitmap(fg, 0f, 0f, null)
        }
        resultBitmap = canvas
        b.imageResult.setImageBitmap(canvas)
        b.btnSave.visibility = View.VISIBLE
        b.txtStatus.text = "✅ Background badal gaya — save karo!"
    }
}
