package com.ypg.neville.model.db.room

import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType
import java.io.File

class NotaRepository(private val notaDao: NotaDao) {

    fun insertar(
        titulo: String,
        nota: String,
        isFav: Boolean = false,
        categoria: String = "",
        isChecklist: Boolean = false,
        checklistJson: String = "",
        syncId: String = "",
        voiceAudioData: ByteArray? = null,
        voiceAudioFileExtension: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        val item = SecureRoomText.encryptNota(NotaEntity(
            titulo = titulo,
            nota = nota,
            fechaCreacion = now,
            fechaModificacion = now,
            isFav = isFav,
            categoria = categoria.trim(),
            isChecklist = isChecklist,
            checklistJson = checklistJson,
            syncId = syncId,
            voiceAudioData = voiceAudioData,
            voiceAudioFileExtension = voiceAudioFileExtension
        ))
        val id = notaDao.insert(item)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.NOTES_CREATED, targetKey = id.toString())
        if (nota.startsWith(NotaEntity.VOICE_NOTE_MARKER)) {
            WeeklySummaryEventLogger.log(WeeklySummaryEventType.VOICE_CREATED, targetKey = id.toString())
        }
        return id
    }

    fun actualizar(
        id: Long,
        titulo: String,
        nota: String,
        fechaCreacionOriginal: Long,
        isFav: Boolean = false,
        categoria: String = "",
        isChecklist: Boolean = false,
        checklistJson: String = "",
        voiceAudioData: ByteArray? = null,
        voiceAudioFileExtension: String = ""
    ) {
        val existing = notaDao.getById(id)?.let(SecureRoomText::decryptNota)
        val syncId = existing?.syncId.orEmpty()
        val resolvedAudio = voiceAudioData ?: existing?.voiceAudioData
        val resolvedExtension = voiceAudioFileExtension.ifBlank {
            existing?.voiceAudioFileExtension.orEmpty()
        }
        val item = SecureRoomText.encryptNota(NotaEntity(
            id = id,
            titulo = titulo,
            nota = nota,
            fechaCreacion = fechaCreacionOriginal,
            fechaModificacion = System.currentTimeMillis(),
            isFav = isFav,
            categoria = categoria.trim(),
            isChecklist = isChecklist,
            checklistJson = checklistJson,
            syncId = syncId,
            voiceAudioData = resolvedAudio,
            voiceAudioFileExtension = resolvedExtension
        ))
        notaDao.update(item)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.NOTES_MODIFIED, targetKey = id.toString())
    }

    fun cambiarFavorito(id: Long, isFav: Boolean) {
        notaDao.updateFavoritoById(id = id, isFav = isFav)
    }

    fun cambiarCategoria(nota: NotaEntity, categoria: String) {
        actualizar(
            id = nota.id,
            titulo = nota.titulo,
            nota = nota.nota,
            fechaCreacionOriginal = nota.fechaCreacion,
            isFav = nota.isFav,
            categoria = categoria,
            isChecklist = nota.isChecklist,
            checklistJson = nota.checklistJson,
            voiceAudioData = nota.voiceAudioData,
            voiceAudioFileExtension = nota.voiceAudioFileExtension
        )
    }

    fun eliminar(nota: NotaEntity) {
        notaDao.delete(SecureRoomText.encryptNota(nota))
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.NOTES_DELETED, targetKey = nota.id.toString())
        if (nota.isVoiceNote) {
            WeeklySummaryEventLogger.log(WeeklySummaryEventType.VOICE_DELETED, targetKey = nota.id.toString())
        }
    }

    fun obtenerTodas(): List<NotaEntity> {
        return notaDao.getAll().map { stored ->
            val nota = SecureRoomText.decryptNota(stored)
            migrateLegacyAudioPathIfNeeded(nota)
        }
    }

    @Suppress("unused")
    fun obtenerPorId(id: Long): NotaEntity? = notaDao.getById(id)?.let(SecureRoomText::decryptNota)

    private fun migrateLegacyAudioPathIfNeeded(nota: NotaEntity): NotaEntity {
        if (nota.hasVoiceAudio || nota.legacyAudioPath.isBlank()) return nota
        val file = File(nota.legacyAudioPath)
        if (!file.isFile) {
            val migratedWithoutAudio = nota.copy(
                nota = if (nota.isVoiceNote) nota.nota else NotaEntity.voiceBody(nota.nota),
                legacyAudioPath = ""
            )
            notaDao.update(SecureRoomText.encryptNota(migratedWithoutAudio))
            return migratedWithoutAudio
        }
        val audio = runCatching { file.readBytes() }
            .getOrNull()
            ?.takeIf(ByteArray::isNotEmpty)
            ?: return nota
        val migrated = nota.copy(
            nota = if (nota.isVoiceNote) nota.nota else NotaEntity.voiceBody(nota.nota),
            legacyAudioPath = "",
            voiceAudioData = audio,
            voiceAudioFileExtension = file.extension.ifBlank { "m4a" }
        )
        notaDao.update(SecureRoomText.encryptNota(migrated))
        file.delete()
        return migrated
    }
}
