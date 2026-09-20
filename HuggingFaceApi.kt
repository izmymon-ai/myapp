package com.ahmed.agentapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * All AI features use Hugging Face's free Inference API.
 * Get a FREE token at: https://huggingface.co/settings/tokens
 * Free tier: ~1000 requests/day — no credit card needed.
 */
object HuggingFaceApi {

    // ─── PUT YOUR FREE TOKEN HERE ───────────────────────────────────────────
    // Get it free from: https://huggingface.co/settings/tokens
    // It looks like: hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    var API_TOKEN = "hf_YOUR_TOKEN_HERE"
    // ────────────────────────────────────────────────────────────────────────

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private const val BASE = "https://api-inference.huggingface.co/models"

    // ── 1. TEXT → IMAGE (Stable Diffusion) ──────────────────────────────────
    fun generateImage(
        prompt: String,
        onResult: (Bitmap?) -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("guidance_scale", 7.5)
                put("num_inference_steps", 25)
            })
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$BASE/stabilityai/stable-diffusion-2-1")
            .header("Authorization", "Bearer $API_TOKEN")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "Network error")
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if (response.isSuccessful && bytes != null) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    onResult(bmp)
                } else {
                    onError("Error ${response.code}: ${response.body?.string()}")
                }
            }
        })
    }

    // ── 2. BACKGROUND REMOVAL ───────────────────────────────────────────────
    fun removeBackground(
        bitmap: Bitmap,
        onResult: (Bitmap?) -> Unit,
        onError: (String) -> Unit
    ) {
        val b64 = bitmapToBase64(bitmap)
        val body = JSONObject().apply {
            put("inputs", b64)
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$BASE/briaai/RMBG-1.4")
            .header("Authorization", "Bearer $API_TOKEN")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "Network error")
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if (response.isSuccessful && bytes != null) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    onResult(bmp)
                } else {
                    onError("Error ${response.code}: ${response.body?.string()}")
                }
            }
        })
    }

    // ── 3. IMAGE UPSCALE / ENHANCE ──────────────────────────────────────────
    fun enhanceImage(
        bitmap: Bitmap,
        onResult: (Bitmap?) -> Unit,
        onError: (String) -> Unit
    ) {
        val b64 = bitmapToBase64(bitmap)
        val body = JSONObject().apply {
            put("inputs", b64)
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$BASE/caidas/swin2SR-classical-sr-x2-64")
            .header("Authorization", "Bearer $API_TOKEN")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "Network error")
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if (response.isSuccessful && bytes != null) {
                    onResult(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                } else {
                    onError("Error ${response.code}: ${response.body?.string()}")
                }
            }
        })
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
