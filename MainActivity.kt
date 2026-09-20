package com.ahmed.agentapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.agentapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.cardImageGen.setOnClickListener   { startActivity(Intent(this, ImageGenActivity::class.java)) }
        b.cardBgChange.setOnClickListener   { startActivity(Intent(this, BgChangeActivity::class.java)) }
        b.cardFilter.setOnClickListener     { startActivity(Intent(this, FilterActivity::class.java)) }
        b.cardVideoMaker.setOnClickListener { startActivity(Intent(this, VideoMakerActivity::class.java)) }
        b.cardSearch.setOnClickListener     { startActivity(Intent(this, SearchActivity::class.java)) }

        // API token setup
        b.btnApiToken.setOnClickListener {
            val token = b.editToken.text.toString().trim()
            if (token.startsWith("hf_") && token.length > 10) {
                HuggingFaceApi.API_TOKEN = token
                b.txtTokenStatus.text = "✅ Token set! Sab features ab kaam karenge."
            } else {
                b.txtTokenStatus.text = "❌ Token galat hai — hf_ se shuru hona chahiye"
            }
        }
    }
}
