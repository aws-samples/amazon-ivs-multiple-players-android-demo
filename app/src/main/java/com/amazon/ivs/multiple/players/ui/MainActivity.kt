package com.amazon.ivs.multiple.players.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amazon.ivs.multiple.players.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        finish()
    }
}
