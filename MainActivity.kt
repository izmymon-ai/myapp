package com.ahmed.aiagent

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ahmed.aiagent.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.cardImageGen.setOnClickListener  { go(ImageGenActivity::class.java) }
        b.cardBgChange.setOnClickListener  { go(BgChangeActivity::class.java) }
        b.cardFilter.setOnClickListener    { go(FilterActivity::class.java) }
        b.cardSearch.setOnClickListener    { go(SearchActivity::class.java) }
    }

    private fun go(cls: Class<*>) = startActivity(Intent(this, cls))
}
