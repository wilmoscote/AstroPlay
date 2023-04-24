package com.wm.astroplay.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wm.astroplay.databinding.ActivityDmcaBinding

class DmcaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDmcaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDmcaBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}