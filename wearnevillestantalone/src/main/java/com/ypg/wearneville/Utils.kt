package com.ypg.wearneville

import android.content.Context
import android.widget.Toast
import java.util.Random

object Utils {

    // Devuelve una frase al azar, tomada de una lista xml
    @JvmStatic
    fun frases(context: Context): String {
        val yArray = context.resources.getStringArray(R.array.list_frases)
        val r = Random()
        return yArray[r.nextInt(yArray.size)]
    }

    // Muestra un mensaje
    @JvmStatic
    fun showmessage(msg: String, context: Context) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
