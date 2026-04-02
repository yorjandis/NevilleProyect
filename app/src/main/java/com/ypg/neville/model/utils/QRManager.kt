package com.ypg.neville.model.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder

object QRManager {

    @JvmField
    var Request_Code = false

    @JvmStatic
    fun ShowQRDialog(context: Context, textoQR: String, title: String, message: String?) {
        if (textoQR.length > 4000) {
            Toast.makeText(context, "Se ha sobrepasado el límite de texto para QR", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = generarQR(textoQR)
        val compose = ComposeView(context)
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message ?: "")
            .setView(compose)
            .create()

        compose.setContent {
            MaterialTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR",
                            modifier = Modifier.size(300.dp)
                        )
                    }

                    Text(
                        text = "Compartir texto",
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, textoQR)
                            context.startActivity(Intent.createChooser(intent, title))
                        }
                    )
                }
            }
        }

        alertDialog.show()
    }

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
