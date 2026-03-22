package com.ypg.neville

import android.app.Activity
import android.os.Bundle
import com.ypg.neville.databinding.ActivityWearMainBinding
import java.util.ArrayList
import java.util.Random

class WearMainActivity : Activity() {

    private lateinit var binding: ActivityWearMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWearMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        frases()

        binding.text.setOnClickListener {
            frases()
        }

        binding.texttitle.setOnClickListener {
        }
    }

    fun frases() {
        val tagList = ArrayList<String>()
        // val someArray = resources.getStringArray(R.array.listfrases)
        val r = Random()
        // binding.text.text = someArray[r.nextInt(someArray.size)]
    }
}
