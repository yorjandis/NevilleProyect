package com.ypg.wearneville

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.ypg.wearneville.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.text.text = Utils.frases(applicationContext)

        binding.text.setOnClickListener {
            binding.text.text = Utils.frases(applicationContext)
            startActivity(Intent(applicationContext, yor2::class.java))
        }
    }

    private fun showmsg() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Yorjandis")
        alertDialog.setMessage("Esto es solo un ejemplo")
        alertDialog.setPositiveButton("Salir") { _, _ ->
            Utils.showmessage("Yorjandis", applicationContext)
        }
        alertDialog.setNegativeButton("yor") { _, _ ->
            Utils.showmessage("perez", applicationContext)
        }
        alertDialog.show()
    }
}
