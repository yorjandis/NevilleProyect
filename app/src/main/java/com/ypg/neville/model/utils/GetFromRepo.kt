package com.ypg.neville.model.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.Executors

// Clase de métodos estáticos que obtendrá datos de diversas fuentes (internas y externas)
class GetFromRepo(var context: Context) {

    companion object {
        // Obtiene la lista de frases desde una página web: Devuelve el arreglo de frases
        @JvmStatic
        fun getFrasesFromWeb(context: Context) {
            if (Utils.isConnection(context)) {
                val exec = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                exec.execute {
                    val url = "https://projectsypg.mozello.com/productos/neville/frases-de-neville/"
                    var noFrasesAnadidas: Long = 0 // número de frases añadidas correctamente a la BD
                    var noFrasesConError: Long = 0 // número de frases que NO han podido importarse a la BD
                    var error = false
                    try {
                        val document = Jsoup.connect(url).maxBodySize(0).get()
                        var ele = document.select("div.yor").outerHtml() // devuelve el código html
                        ele = ele.replace("<p class=\"", "") // quitando parte de la linea
                        ele = ele.replace("</p>", "") // Quitando el final de las lineas
                        ele = ele.replace("</div>", "") // quitando la última cadena

                        val arrayFrase = ele.split("\n".toRegex()).toTypedArray() // diviendo el texto en líneas

                        for (i in 1 until arrayFrase.size) {
                            val temp = arrayFrase[i].split("\">".toRegex()).toTypedArray() // autor y texto de frase
                            val result = utilsDB.insertNewFrase(context, temp[1], temp[0], "", "1") // Inserta una frase

                            // Obteniendo estadísticas del resultado
                            if (result > 0) {
                                noFrasesAnadidas += 1
                            } else {
                                noFrasesConError += 1
                            }
                        }
                    } catch (_: IOException) {
                        error = true
                    }

                    // UI thread
                    val finalError = error
                    val finalResult = noFrasesAnadidas
                    val finalResultError = noFrasesConError

                    // Chequeando si es un contexto UI válido
                    if (context is MainActivity) {
                        if (context.isFinishing) return@execute
                    }

                    handler.post {
                        if (finalError) {
                            Toast.makeText(context, "Se produjo un error, código: 001", Toast.LENGTH_SHORT).show()
                        } else {
                            if (finalResult > 0) {
                                Toast.makeText(context, "Se han añadido: $finalResult Frases nuevas", Toast.LENGTH_SHORT).show()
                            }
                            if (finalResultError > 0) {
                                Toast.makeText(context, "No se han añadido: $finalResultError Frases", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Error al importar frases. No se detecta conexión internet", Toast.LENGTH_SHORT).show()
            }
        }

        // Obtiene un arreglo con la lista de frases del fichero xml interno
        @JvmStatic
        fun getFrasesFromXML(context: Context): Array<String> {
            return context.resources.getStringArray(R.array.list_frases)
        }

        // Obtiene un arreglo con la lista de url de videos de youtube del fichero xml interno
        @JvmStatic
        @Suppress("unused")
        fun getUrlsVideosFromXML(context: Context): Array<String> {
            return context.resources.getStringArray(R.array.listvideos)
        }

        // Obtiene un arreglo con la lista de conferencias del directorio Assets interno
        @JvmStatic
        @Throws(IOException::class)
        fun getConfListFromAssets(context: Context): Array<String>? {
            return context.assets
                .list("autores/neville/conf")
                ?.filter { it.startsWith("conf_") && it.endsWith(".txt", ignoreCase = true) }
                ?.sorted()
                ?.toTypedArray()
        }
    }
}
