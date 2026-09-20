package com.ahmed.aiagent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object PollinationsApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun generateImage(
        prompt: String,
        onResult: (Bitmap?) -> Unit,
        onError: (String) -> Unit
    ) {
        val encoded = URLEncoder.encode(prompt, "UTF-8")
        val seed    = Random.nextInt(99999)
        val url     = "https://image.pollinations.ai/prompt/$encoded?width=768&height=768&nologo=true&seed=$seed"

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Internet check karo: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if (response.isSuccessful && bytes != null && bytes.isNotEmpty()) {
                    onResult(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                } else {
                    onError("Nahi bani (${response.code}) — dobara try karo")
                }
            }
        })
    }
}
