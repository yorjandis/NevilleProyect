package com.ypg.neville.model.frases

import android.content.Context

data class ParsedFrase(
    val assetKey: String,
    val autor: String,
    val frase: String,
    val fuente: String,
    val nota: String,
    val categoria: String,
    val assetHash: String
)

object FrasesAssetParser {

    const val CATEGORIA_AUTOR = "AUTOR"
    const val CATEGORIA_OTROS = "OTROS"
    const val CATEGORIA_SALUD = "SALUD"

    data class SourceSpec(
        val assetPath: String,
        val categoria: String
    )

    val sourceSpecs = listOf(
        SourceSpec("frases/listfrases.txt", CATEGORIA_AUTOR),
        SourceSpec("frases/listfrases_bruce.txt", CATEGORIA_AUTOR),
        SourceSpec("frases/listfrases_gregg.txt", CATEGORIA_AUTOR),
        SourceSpec("frases/listfrases_jd.txt", CATEGORIA_AUTOR),
        SourceSpec("frases/listfrases_otros.txt", CATEGORIA_OTROS),
        SourceSpec("frases/listfrases_salud.txt", CATEGORIA_SALUD)
    )

    fun parse(context: Context, spec: SourceSpec): List<ParsedFrase> {
        val raw = context.assets.open(spec.assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val normalizedText = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()

        if (normalizedText.isBlank()) return emptyList()

        val result = mutableListOf<ParsedFrase>()
        val fields = linkedMapOf<String, String>()
        val knownFields = setOf("id", "autor", "nota", "fuente", "contexto", "texto", "relacionadas")
        var currentField: String? = null
        var fallbackIndex = 0

        fun flushBlock() {
            if (fields.isEmpty()) return
            fallbackIndex += 1
            val rawId = fields["id"].orEmpty().trim()
            val assetKey = if (rawId.isNotBlank()) rawId else "${spec.assetPath}::$fallbackIndex"
            val rawAutor = fields["autor"].orEmpty().trim()
            val autor = normalizeAutor(rawAutor, spec.categoria)
            val frase = fields["texto"].orEmpty().trim()
            if (frase.isNotBlank()) {
                result.add(
                    ParsedFrase(
                        assetKey = assetKey,
                        autor = autor,
                        frase = frase,
                        fuente = fields["fuente"].orEmpty().trim(),
                        nota = fields["nota"].orEmpty().trim(),
                        categoria = spec.categoria,
                        assetHash = ""
                    )
                )
            }
            fields.clear()
            currentField = null
        }

        for (lineRaw in normalizedText.lines()) {
            val line = lineRaw.trimEnd()
            if (line.startsWith("id=") && fields.isNotEmpty()) {
                flushBlock()
            }

            val sep = line.indexOf('=')
            val key = if (sep > 0) line.substring(0, sep).trim() else ""
            if (sep > 0 && key in knownFields) {
                val value = line.substring(sep + 1).trim()
                fields[key] = value
                currentField = key
                continue
            }

            if (line.isBlank()) continue
            val field = currentField ?: continue
            val previous = fields[field].orEmpty()
            fields[field] = if (previous.isBlank()) line.trim() else "$previous\n${line.trim()}"
        }

        flushBlock()
        return result
    }

    private fun normalizeAutor(rawAutor: String, categoria: String): String {
        if (categoria == CATEGORIA_SALUD) return "Salud"

        return when (rawAutor.trim().lowercase()) {
            "nev", "neville", "nevillegoddard", "neville goddard" -> "Neville Goddard"
            "brucel", "bruce", "bruce lipton" -> "Bruce Lipton"
            "gregg", "gregg braden" -> "Gregg Braden"
            "jd", "joe dispenza", "joedispenza" -> "Joe Dispenza"
            "salud" -> "Salud"
            else -> rawAutor.ifBlank { "Autor desconocido" }
        }
    }
}
