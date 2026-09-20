package com.ahmed.agentapp

import android.content.ContentValues
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.agentapp.databinding.ActivityVideoMakerBinding
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer

class VideoMakerActivity : AppCompatActivity() {

    private lateinit var b: ActivityVideoMakerBinding
    private val selectedImages = mutableListOf<Bitmap>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages.clear()
        uris.forEach { uri ->
            contentResolver.openInputStream(uri)?.let {
                BitmapFactory.decodeStream(it)?.let { bmp -> selectedImages.add(bmp) }
            }
        }
        b.txtImageCount.text = "${selectedImages.size} images selected"
        if (selectedImages.isNotEmpty()) b.btnMakeVideo.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityVideoMakerBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPickImages.setOnClickListener { pickImages.launch("image/*") }

        b.btnMakeVideo.setOnClickListener {
            if (selectedImages.isEmpty()) return@setOnClickListener
            val durationMs = (b.seekDuration.progress + 1) * 1000
            makeVideo(durationMs)
        }
    }

    private fun makeVideo(frameMs: Int) {
        b.progressBar.visibility = View.VISIBLE
        b.btnMakeVideo.isEnabled = false
        b.txtStatus.text = "Video ban rahi hai..."

        scope.launch {
            try {
                val outputFile = File(
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "AgentApp_Video_${System.currentTimeMillis()}.mp4"
                )

                val W = 1280; val H = 720
                val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()

                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var trackIndex = -1
                var muxerStarted = false
                val bufferInfo = MediaCodec.BufferInfo()
                var presentationTimeUs = 0L
                val frameDurationUs = (frameMs * 1000L)
                val framesPerImage = (frameMs / 1000.0 * 30).toInt().coerceAtLeast(30)

                for (bitmap in selectedImages) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, W, H, true)
                    val yuv = bitmapToYUV420(scaled, W, H)
                    scaled.recycle()

                    repeat(framesPerImage) {
                        val inputIndex = withContext(Dispatchers.IO) {
                            encoder.dequeueInputBuffer(10_000)
                        }
                        if (inputIndex >= 0) {
                            val buf = encoder.getInputBuffer(inputIndex)!!
                            buf.clear()
                            buf.put(yuv)
                            encoder.queueInputBuffer(inputIndex, 0, yuv.size, presentationTimeUs, 0)
                            presentationTimeUs += 33_333L // ~30fps
                        }

                        var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                        while (outputIndex >= 0 || outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                trackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            } else if (outputIndex >= 0) {
                                val buf = encoder.getOutputBuffer(outputIndex)!!
                                if (muxerStarted && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                    muxer.writeSampleData(trackIndex, buf, bufferInfo)
                                }
                                encoder.releaseOutputBuffer(outputIndex, false)
                            }
                            outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                        }
                    }
                }

                // Flush
                val inputIndex = withContext(Dispatchers.IO) { encoder.dequeueInputBuffer(10_000) }
                if (inputIndex >= 0) {
                    encoder.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
                var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                while (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0) {
                    if (outputIndex >= 0) {
                        val buf = encoder.getOutputBuffer(outputIndex)!!
                        if (muxerStarted && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            muxer.writeSampleData(trackIndex, buf, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                    outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                }

                encoder.stop(); encoder.release(); muxer.stop(); muxer.release()

                // Save to gallery
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outputFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AgentApp")
                }
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputFile.inputStream().copyTo(it) }
                }

                b.progressBar.visibility = View.GONE
                b.btnMakeVideo.isEnabled = true
                b.txtStatus.text = "✅ Video tayyar! Gallery > Movies > AgentApp mein dekho."
                Toast.makeText(this@VideoMakerActivity, "✅ Video ban gayi!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                b.progressBar.visibility = View.GONE
                b.btnMakeVideo.isEnabled = true
                b.txtStatus.text = "❌ Error: ${e.message}"
            }
        }
    }

    private fun bitmapToYUV420(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val yuv = ByteArray(width * height * 3 / 2)
        var yIndex = 0; var uvIndex = width * height
        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b_ = pixel and 0xFF
                val y = ((66 * r + 129 * g + 25 * b_ + 128) shr 8) + 16
                yuv[yIndex++] = y.toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b_ + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b_ + 128) shr 8) + 128
                    yuv[uvIndex++] = u.toByte()
                    yuv[uvIndex++] = v.toByte()
                }
            }
        }
        return yuv
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
