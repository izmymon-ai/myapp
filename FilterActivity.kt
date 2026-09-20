package com.ahmed.aiagent

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.aiagent.databinding.ActivityFilterBinding

class FilterActivity : AppCompatActivity() {

    private lateinit var b: ActivityFilterBinding
    private var original: Bitmap? = null
    private var current: Bitmap? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = contentResolver.openInputStream(it)
                ?.let { s -> BitmapFactory.decodeStream(s) } ?: return@let
            original = bmp
            current  = bmp.copy(Bitmap.Config.ARGB_8888, true)
            b.imageView.setImageBitmap(current)
            b.filterRow.visibility = View.VISIBLE
            b.btnSave.visibility   = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPick.setOnClickListener    { pickImage.launch("image/*") }
        b.btnOriginal.setOnClickListener { reset() }
        b.btnBW.setOnClickListener       { filter(ColorMatrix().also { it.setSaturation(0f) }) }
        b.btnSepia.setOnClickListener    {
            filter(ColorMatrix(floatArrayOf(
                0.393f,0.769f,0.189f,0f,0f,
                0.349f,0.686f,0.168f,0f,0f,
                0.272f,0.534f,0.131f,0f,0f,
                0f,0f,0f,1f,0f)))
        }
        b.btnWarm.setOnClickListener     { colorScale(1.3f,1.0f,0.7f) }
        b.btnCool.setOnClickListener     { colorScale(0.7f,1.0f,1.4f) }
        b.btnVivid.setOnClickListener    { colorScale(1.3f,1.3f,1.3f) }
        b.btnNeg.setOnClickListener      {
            filter(ColorMatrix(floatArrayOf(
                -1f,0f,0f,0f,255f,
                0f,-1f,0f,0f,255f,
                0f,0f,-1f,0f,255f,
                0f,0f,0f,1f,0f)))
        }

        b.seekBright.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) brightness(p - 127)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        b.btnSave.setOnClickListener {
            val bmp = current ?: return@setOnClickListener
            val ok = ImageSaveHelper.save(this, bmp, "Filtered")
            Toast.makeText(this,
                if (ok) "✅ Gallery mein save!" else "❌ Save nahi hua",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun reset() {
        current = original?.copy(Bitmap.Config.ARGB_8888, true)
        b.imageView.setImageBitmap(current)
        b.seekBright.progress = 127
    }

    private fun filter(matrix: ColorMatrix) {
        val src = original ?: return
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        })
        current = out
        b.imageView.setImageBitmap(out)
    }

    private fun colorScale(r: Float, g: Float, bl: Float) {
        filter(ColorMatrix(floatArrayOf(
            r,0f,0f,0f,0f,  0f,g,0f,0f,0f,
            0f,0f,bl,0f,0f, 0f,0f,0f,1f,0f)))
    }

    private fun brightness(v: Int) {
        val vf = v.toFloat()
        filter(ColorMatrix(floatArrayOf(
            1f,0f,0f,0f,vf, 0f,1f,0f,0f,vf,
            0f,0f,1f,0f,vf, 0f,0f,0f,1f,0f)))
    }
}
