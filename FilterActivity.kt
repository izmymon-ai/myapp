package com.ahmed.agentapp

import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.agentapp.databinding.ActivityFilterBinding

class FilterActivity : AppCompatActivity() {

    private lateinit var b: ActivityFilterBinding
    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = contentResolver.openInputStream(it)
            originalBitmap = BitmapFactory.decodeStream(stream)
            currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            b.imageView.setImageBitmap(currentBitmap)
            b.controls.visibility = View.VISIBLE
            b.btnSave.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        // Filters
        b.btnOriginal.setOnClickListener    { resetToOriginal() }
        b.btnGrayscale.setOnClickListener   { applyGrayscale() }
        b.btnSepia.setOnClickListener       { applySepia() }
        b.btnNegative.setOnClickListener    { applyNegative() }
        b.btnWarm.setOnClickListener        { applyColorFilter(1.3f, 1.0f, 0.7f) }   // warm
        b.btnCool.setOnClickListener        { applyColorFilter(0.7f, 1.0f, 1.4f) }   // cool
        b.btnVivid.setOnClickListener       { applyColorFilter(1.3f, 1.3f, 1.3f) }   // vivid

        // Enhance with AI
        b.btnEnhanceAi.setOnClickListener {
            val bmp = currentBitmap ?: return@setOnClickListener
            b.txtStatus.text = "AI se enhance ho raha hai..."
            HuggingFaceApi.enhanceImage(bmp,
                onResult = { enhanced ->
                    runOnUiThread {
                        enhanced?.let {
                            currentBitmap = it
                            b.imageView.setImageBitmap(it)
                            b.txtStatus.text = "✅ AI enhance ho gayi!"
                        } ?: run { b.txtStatus.text = "❌ Enhance nahi hua" }
                    }
                },
                onError = { err -> runOnUiThread { b.txtStatus.text = "❌ $err" } }
            )
        }

        // Brightness seekbar
        b.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) applyBrightness(progress - 50)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        b.btnSave.setOnClickListener { saveImage() }
    }

    private fun resetToOriginal() {
        currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        b.imageView.setImageBitmap(currentBitmap)
        b.seekBrightness.progress = 50
    }

    private fun applyGrayscale() {
        val bmp = originalBitmap ?: return
        val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        currentBitmap = result
        b.imageView.setImageBitmap(result)
    }

    private fun applySepia() {
        val bmp = originalBitmap ?: return
        val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        currentBitmap = result
        b.imageView.setImageBitmap(result)
    }

    private fun applyNegative() {
        val bmp = originalBitmap ?: return
        val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
             0f,-1f, 0f, 0f, 255f,
             0f, 0f,-1f, 0f, 255f,
             0f, 0f, 0f, 1f,   0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        currentBitmap = result
        b.imageView.setImageBitmap(result)
    }

    private fun applyColorFilter(r: Float, g: Float, b_: Float) {
        val bmp = originalBitmap ?: return
        val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix(floatArrayOf(
            r,  0f, 0f, 0f, 0f,
            0f, g,  0f, 0f, 0f,
            0f, 0f, b_, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        currentBitmap = result
        b.imageView.setImageBitmap(result)
    }

    private fun applyBrightness(amount: Int) {
        val bmp = originalBitmap ?: return
        val v = amount.toFloat()
        val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, v,
            0f, 1f, 0f, 0f, v,
            0f, 0f, 1f, 0f, v,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        currentBitmap = result
        b.imageView.setImageBitmap(result)
    }

    private fun saveImage() {
        val bmp = currentBitmap ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Filtered_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AgentApp")
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            Toast.makeText(this, "✅ Gallery mein save ho gayi!", Toast.LENGTH_SHORT).show()
        }
    }
}
