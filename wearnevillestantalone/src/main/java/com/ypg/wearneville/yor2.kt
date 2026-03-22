package com.ypg.wearneville

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class yor2 : Activity(), View.OnClickListener {

    private lateinit var button: Button
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yor2)

        button = findViewById(R.id.btnyor)
        textView = findViewById(R.id.text1)

        button.setOnClickListener(this)
        textView.setOnClickListener(this)

        // centrando el texto en el centro de la pantalla
        textView.text = Utils.frases(applicationContext)

        setParamText()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnyor -> {
            }
            R.id.text1 -> {
                textView.text = Utils.frases(applicationContext)
            }
        }
    }

    // centrando el texto en la pantalla:
    private fun setParamText() {
        val wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)

        val params = textView.layoutParams as LinearLayout.LayoutParams
        params.width = size.x
        textView.layoutParams = params

        textView.gravity = Gravity.CENTER
    }
}
