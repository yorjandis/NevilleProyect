package com.ypg.neville.model.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.services.serviceStreaming
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList

// Clase para operaciones generales
class Utils(private val context: Context) {

    companion object {
        /**
         * @param packageName Nombre del paquete
         * @param context contexto
         * @return Devuelve true si existe la app, false de lo contrario
         */
        @JvmStatic
        fun isPackageInstalled(packageName: String, context: Context): Boolean {
            val pm = context.packageManager
            return try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        /**
         * Chequea si existe conexión a internet. Envía un mensaje de NO haber conexión
         * @param contextP Contexto
         * @return true si existe conexión, false de lo contrario
         */
        @JvmStatic
        fun isConnection(contextP: Context): Boolean {
            val connectivityManager = contextP.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            val isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected

            if (!isConnected) {
                Toast.makeText(contextP, "Contenido no accesible sin conexión", Toast.LENGTH_SHORT).show()
            }
            return isConnected
        }

        /**
         * Inicia el servicio de streaming reproduciendo un medio en particular (solo para medios offline)
         * @param context contexto
         * @param repoDir nombre del directorio repo
         * @param ItemName nombre del fichero a reproducir
         */
        @JvmStatic
        fun playInStreaming(context: Context, repoDir: String, ItemName: String) {
            val intent = Intent(context, serviceStreaming::class.java)
            context.applicationContext.stopService(intent) // detiene el servicio

            intent.action = "play_medio"
            intent.putExtra("file", utilsFields.PATH_ROOT_REPO + repoDir + File.separator + ItemName)
            intent.putExtra("title", ItemName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        // Carga el contenido de los directorios externos. Genera el listado para los archivos en las carpetas "videos" y "audios"
        @JvmStatic
        fun loadRepo(context: Context) {
            val alert = AlertDialog.Builder(context)
            alert.setTitle("Indexar el contenido offline")
            alert.setMessage(" Utilize esta opción Si ha cambiado el contenido en la carpeta NevilleRepo en su almacenamiento externo")
            alert.setPositiveButton("Actualizar") { _, _ ->
                utilsDB.popularDB_Repo(context) // Actualiza la tabla repo con el nuevo contenido en las carpetas del sistema
                Toast.makeText(context, "El contenido se ha actualizado", Toast.LENGTH_SHORT).show()
            }
            alert.show()
        }

        // Devuelve una cadena legible de elementos que pueden tener una nota asociada
        @JvmStatic
        fun getElementLoad(key: String): String {
            return when (key) {
                "frases" -> "Esta frase"
                "conf" -> "Esta Conferencia"
                "video_conf" -> "Este Video Conferencia"
                "video_gredd" -> "Video gregg"
                "video_book" -> "Este Audio libro"
                "video_ext" -> "Este video offline"
                "audio_ext" -> "Este Audio offline"
                else -> ""
            }
        }

        // Crear la estructura de directorios
        @JvmStatic
        fun CrearDirectoriosRepo(pcontext: Context) {
            // Creando la estructura de directorios en el almacenamiento externo, Si no existe
            val path = Environment.getExternalStoragePublicDirectory(utilsFields.REPO_DIR_ROOT).toString() + File.separator + utilsFields.REPO_DIR_VIDEOS
            val file = File(path)
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    Toast.makeText(pcontext, "Directorio Videos NO creado", Toast.LENGTH_SHORT).show()
                }
            }
            val path2 = Environment.getExternalStoragePublicDirectory(utilsFields.REPO_DIR_ROOT).toString() + File.separator + utilsFields.REPO_DIR_AUDIOS
            val file2 = File(path2)
            if (!file2.exists()) {
                if (!file2.mkdirs()) {
                    Toast.makeText(pcontext, "Directorio Audios NO creado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // devuelve una lista de conferencias en Assets que contiene una subcadena de texto
        @JvmStatic
        @Throws(IOException::class)
        fun searchInConf(pcontext: Context, string: String): List<String> {
            val listFiles = pcontext.assets.list("conf") ?: return emptyList()
            var `is`: InputStream
            var confText: String
            var size: Int
            var buffer: ByteArray
            val lista: MutableList<String> = LinkedList()

            for (file in listFiles) {
                `is` = pcontext.assets.open("conf" + File.separator + file)
                size = `is`.available()
                buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                confText = String(buffer)

                if (confText.contains(string, ignoreCase = true)) {
                    lista.add(file.replace(".txt", ""))
                }
            }
            return lista
        }
    }

    /**
     * Lista los ficheros en el directorio assets y sus carpetas
     * @param dir puede ser "" para la raiz de assets o el nombre de una subcarpeta como "config"
     * @param lista Objeto de tipo List que es devuelto con los datos
     * @throws IOException Puede larzar una excepción de IO
     */
    @Throws(IOException::class)
    fun listFilesInAssets(dir: String, lista: MutableList<String>) {
        lista.clear() // limpia el arreglo de String
        val fileList = context.assets.list(dir) ?: return
        for (file in fileList) {
            lista.add(file.replace(".txt", ""))
        }
    }

    /**
     * Muestra una notificación en la barra de tareas
     * @param bigtext texto grande
     * @param intent intent
     */
    @TargetApi(Build.VERSION_CODES.O)
    fun show_Notification(bigtext: String, intent: Intent) {
        val notificationChannel = NotificationChannel(utilsFields.NOTIFICACION_CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW)
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = Notification.Builder(context, utilsFields.NOTIFICACION_CHANNEL_ID)
            .setContentTitle("")
            .setContentIntent(pendingIntent)
            .setStyle(Notification.BigTextStyle().bigText(bigtext))
            .addAction(android.R.drawable.sym_action_chat, "Actualizar", pendingIntent)
            .setChannelId(utilsFields.NOTIFICACION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        notificationManager.notify(utilsFields.NOTIFICACION_ID, notification)
    }
}
