package com.ypg.neville.model.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.preferences.DbPreferences

object FraseContextActions {

    data class NotaCreadaResult(
        val ok: Boolean,
        val titulo: String
    )

    fun convertirFraseEnNota(context: Context, frase: String): NotaCreadaResult {
        val texto = frase.trim()
        if (texto.isBlank()) return NotaCreadaResult(ok = false, titulo = "")

        val baseTitle = buildNotaTitle(texto)
        var candidate = baseTitle
        var i = 2
        while (utilsDB.getApunteByTitle(context, candidate) != null) {
            candidate = "$baseTitle ($i)"
            i++
        }

        val insert = utilsDB.insertNewApunte(context, candidate, texto)
        return NotaCreadaResult(ok = insert >= 0, titulo = candidate)
    }

    fun abrirNotaDeFrase(context: Context, frase: String) {
        val texto = frase.trim()
        if (texto.isBlank()) {
            Toast.makeText(context, "La frase está vacía", Toast.LENGTH_SHORT).show()
            return
        }
        val nota = utilsDB.getFraseNota(context, texto)
        UiModalWindows.NotaManager(
            context,
            nota,
            DatabaseHelper.T_Frases,
            DatabaseHelper.C_frases_frase,
            texto
        )
    }

    fun cargarFraseEnLienzo(context: Context, frase: String) {
        val texto = frase.trim()
        if (texto.isBlank()) {
            Toast.makeText(context, "La frase está vacía", Toast.LENGTH_SHORT).show()
            return
        }

        DbPreferences.named(context, "lienzo_prefs")
            .edit()
            .putString("textoPrincipal", texto)
            .apply()

        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_lienzo)
    }

    fun compartirFraseSistema(
        context: Context,
        frase: String,
        autor: String,
        fuente: String
    ) {
        val texto = frase.trim()
        if (texto.isBlank()) {
            Toast.makeText(context, "La frase está vacía", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = buildString {
            append(texto)
            if (autor.isNotBlank()) append("\n\n<$autor>")
            if (fuente.isNotBlank()) append("\nFuente: $fuente")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Frase"))
    }

    private fun buildNotaTitle(frase: String): String {
        val normalized = frase.replace("\n", " ").trim()
        if (normalized.isBlank()) return "Frase"
        return normalized.take(20).trim().ifBlank { "Frase" }
    }
}
