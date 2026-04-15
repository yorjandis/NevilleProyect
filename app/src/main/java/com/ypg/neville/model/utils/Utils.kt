package com.ypg.neville.model.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import android.graphics.drawable.Icon
import com.ypg.neville.R
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
            } catch (_: PackageManager.NameNotFoundException) {
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
            val network = connectivityManager.activeNetwork
            val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            if (!isConnected) {
                Toast.makeText(contextP, "Contenido no accesible sin conexión", Toast.LENGTH_SHORT).show()
            }
            return isConnected
        }

        // devuelve una lista de conferencias en Assets que contiene una subcadena de texto
        @JvmStatic
        @Throws(IOException::class)
        fun searchInConf(pcontext: Context, string: String): List<String> {
            val listFiles = pcontext.assets.list("autores/neville/conf") ?: return emptyList()
            var `is`: InputStream
            var confText: String
            var size: Int
            var buffer: ByteArray
            val lista: MutableList<String> = LinkedList()

            for (file in listFiles) {
                if (!file.startsWith("conf_") || !file.endsWith(".txt", ignoreCase = true)) continue

                `is` = pcontext.assets.open("autores/neville/conf" + File.separator + file)
                size = `is`.available()
                buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                confText = String(buffer)

                if (confText.contains(string, ignoreCase = true)) {
                    lista.add(
                        file
                            .removePrefix("conf_")
                            .replace(".txt", "")
                    )
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
    @RequiresApi(Build.VERSION_CODES.O)
    fun show_Notification(bigtext: String, intent: Intent) {
        val notificationChannel = NotificationChannel(utilsFields.NOTIFICACION_CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW)
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        val updateAction = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.sym_action_chat),
            "Actualizar",
            pendingIntent
        ).build()

        val notification = Notification.Builder(context, utilsFields.NOTIFICACION_CHANNEL_ID)
            .setContentTitle("")
            .setContentIntent(pendingIntent)
            .setStyle(Notification.BigTextStyle().bigText(bigtext))
            .addAction(updateAction)
            .setChannelId(utilsFields.NOTIFICACION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_neville)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        notificationManager.notify(utilsFields.NOTIFICACION_ID, notification)
    }
}
