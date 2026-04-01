package com.ypg.neville.model.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.zxing.integration.android.IntentIntegrator
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder

/**
 * Clase que se encarga de manejar la funcionalidad de código QR
 */
object QRManager {

    @JvmField
    var Request_Code = false // para OnactivityResult

    /**
     * Genera el código QR generado y lo muestra en un diálogo
     * @param context contexto
     * @param textoQR texto de qr
     * @param title título del diálogo
     * @param message mensaje del diálogo
     */
    @JvmStatic
    fun ShowQRDialog(context: Context, textoQR: String, title: String, message: String?) {
        // Determinar el número de caracteres para saber si se puede convertir a QR:
        if (textoQR.length > 4000) {
            Toast.makeText(context, "Se ha sobrepasado el límite de texto para QR", Toast.LENGTH_SHORT).show()
            return
        }

        val alert = AlertDialog.Builder(context)
        alert.setTitle(title)
        alert.setMessage(message ?: "")

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_qr, null, false)
        val imageView = view.findViewById<ImageView>(R.id.qr_image_view)
        val imageShared = view.findViewById<ImageView>(R.id.qr_image_shared)

        imageView.setImageBitmap(generarQR(textoQR))

        alert.setView(view)
        val alertDialog = alert.create()
        alertDialog.show()

        imageShared.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, textoQR)
            context.startActivity(Intent.createChooser(intent, title))
        }
    }

    /**
     * Inicializa el lector de QR
     */
    @JvmStatic
    fun launch_QRRead() {
        MainActivity.currentInstance()?.let {
            val intentIntegrator = IntentIntegrator(it)
            intentIntegrator.setPrompt("neville - Leyendo Código QR")
            intentIntegrator.setBarcodeImageEnabled(true)
            intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            intentIntegrator.setBeepEnabled(false)
            Request_Code = true
            intentIntegrator.initiateScan()
        }
    }

    /**
     * Genera un bitmap de QR del texto pasado como parametro
     * @param textP Texto del código QR
     */
    private fun generarQR(textP: String): Bitmap? {
        val qrgEncoder = QRGEncoder(textP, null, QRGContents.Type.TEXT, 300)
        qrgEncoder.colorBlack = Color.WHITE
        qrgEncoder.colorWhite = Color.BLACK

        return try {
            qrgEncoder.bitmap
        } catch (_: Exception) {
            null
        }
    }
}
