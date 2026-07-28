package com.ypg.neville.feature.morningdialog.domain

import com.ypg.neville.feature.morningdialog.data.EveningReviewDao
import com.ypg.neville.feature.morningdialog.data.EveningReviewEntity
import com.ypg.neville.model.db.room.DiarioRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class EveningRitualRepository(
    private val db: NevilleRoomDatabase,
    private val reviewDao: EveningReviewDao = db.eveningReviewDao()
) {
    fun observeReviews(): Flow<List<EveningReview>> =
        reviewDao.observeAll().map { rows -> rows.map(EveningReviewEntity::toDomain) }

    suspend fun reviews(): List<EveningReview> = reviewDao.getAll().map(EveningReviewEntity::toDomain)

    suspend fun reviewForDay(epochDay: Long): EveningReview? = reviewDao.getByDay(epochDay)?.toDomain()

    suspend fun snapshot(startMillis: Long, endMillis: Long): EveningDaySnapshot {
        val agenda = db.agendaItemDao().loadBetween(startMillis, endMillis)
        val completedUnits = db.goalUnitDao().getCompletedBetween(startMillis, endMillis)
        val presenceReturns = db.presenceEventDao().countByTypeBetween(
            type = "presente",
            startMillis = startMillis,
            endMillis = endMillis
        )
        val automaticPilot = db.presenceEventDao().countAutomaticPilotBetween(startMillis, endMillis)
        val coherence = db.meditationSessionRecordDao().getBetween(startMillis, endMillis)
        val scores = coherence.map { it.afterScore }.filter { it > 0 }

        return EveningDaySnapshot(
            agendaCompleted = agenda.filter { it.completed == true }.map {
                CompletedAgendaItem(it.id, it.title.ifBlank { "Actividad completada" })
            },
            agendaTotalCount = agenda.size,
            completedGoalUnits = completedUnits.map {
                CompletedGoalUnit(
                    goalTitle = it.goalTitle,
                    unitName = it.unitName.ifBlank { "Unidad completada" }
                )
            },
            presenceReturns = presenceReturns,
            automaticPilotEvents = automaticPilot,
            coherenceSessionsCount = coherence.size,
            coherenceAverageAfterScore = scores.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        )
    }

    suspend fun saveReview(
        epochDay: Long,
        dayStartMillis: Long,
        draft: EveningReviewDraft,
        snapshot: EveningDaySnapshot,
        morningSession: MorningDialogSession?,
        recentReviews: List<EveningReview>
    ): EveningReview {
        val existing = reviewDao.getByDay(epochDay)?.toDomain()
        val suggestion = suggestedImprovement(
            draft = draft,
            snapshot = snapshot,
            morningSession = morningSession,
            recentReviews = recentReviews
        )
        var journalId = existing?.journalEntryId
        var journalCreated = existing?.journalEntryCreated == true

        if (draft.createJournalEntry) {
            val diary = DiarioRepository(db.diarioDao())
            val title = "Cierre consciente · ${formatDay(dayStartMillis)}"
            val content = buildJournalContent(draft, snapshot, morningSession, suggestion)
            val existingEntry = journalId?.let(diary::obtenerPorId)
            journalId = if (existingEntry == null) {
                diary.insertar(
                    title = title,
                    content = content,
                    emocion = emotionEmoji(draft.predominantEmotionId),
                    capitulo = "Cierre consciente",
                    isFav = false,
                    fechaCreacionMillis = dayStartMillis
                )
            } else {
                diary.actualizar(
                    id = existingEntry.id,
                    title = title,
                    content = content,
                    emocion = emotionEmoji(draft.predominantEmotionId),
                    capitulo = "Cierre consciente",
                    isFav = existingEntry.isFav,
                    fechaOriginal = existingEntry.fecha
                )
                existingEntry.id
            }
            journalCreated = true
        }

        val review = EveningReview(
            id = existing?.id ?: 0,
            sessionDateEpochDay = epochDay,
            completedAtEpochMillis = System.currentTimeMillis(),
            energy = draft.energy.coerceIn(1, 5),
            predominantEmotionId = draft.predominantEmotionId,
            whatWentWell = draft.whatWentWell.trim(),
            learning = draft.learning.trim(),
            autopilotMoment = draft.autopilotMoment.trim(),
            gratitude = draft.gratitude.trim(),
            tomorrowPreparation = draft.tomorrowPreparation.trim(),
            identityAlignment = draft.identityAlignment.coerceIn(1, 5),
            suggestion = suggestion,
            agendaCompletedCount = snapshot.agendaCompleted.size,
            agendaTotalCount = snapshot.agendaTotalCount,
            goalUnitsCompletedCount = snapshot.completedGoalUnits.size,
            presenceReturns = snapshot.presenceReturns,
            automaticPilotEvents = snapshot.automaticPilotEvents,
            coherenceSessionsCount = snapshot.coherenceSessionsCount,
            dayContextFingerprint = snapshot.fingerprint(),
            journalEntryRequested = draft.createJournalEntry,
            journalEntryCreated = journalCreated,
            journalEntryId = journalId
        )
        val storedId = reviewDao.upsert(review.toEntity())
        return review.copy(id = if (review.id == 0L) storedId else review.id)
    }

    suspend fun deleteReview(reviewId: Long) = reviewDao.deleteById(reviewId)

    private fun suggestedImprovement(
        draft: EveningReviewDraft,
        snapshot: EveningDaySnapshot,
        morningSession: MorningDialogSession?,
        recentReviews: List<EveningReview>
    ): String {
        if (recentReviews.size >= 3 && recentReviews.map { it.energy }.average() < 3.0) {
            return "Tu energía lleva varios días baja: deja preparada una sola prioridad y protege un inicio de mañana más lento y realista."
        }
        if (recentReviews.size >= 3 && recentReviews.map { it.automaticPilotEvents }.average() >= 2.0) {
            return "El piloto automático se repite esta semana: elige una señal concreta —una alarma, una respiración o una frase— antes de tu momento más vulnerable."
        }
        if (draft.energy <= 2) {
            return "Protege tu energía: deja preparada solo la primera acción esencial de mañana y date permiso para empezar despacio."
        }
        if (draft.autopilotMoment.isNotBlank() || snapshot.automaticPilotEvents > 0) {
            return "Antes del primer momento que suele llevarte al piloto automático, haz una pausa de tres respiraciones y recuerda tu identidad elegida."
        }
        if (draft.identityAlignment <= 2 && morningSession?.identity?.isNotBlank() == true) {
            return "Mañana lee al despertar: «Hoy actúo como ${morningSession.identity}», y conviértelo en un gesto visible durante la primera hora."
        }
        if (snapshot.agendaTotalCount > 0 && snapshot.agendaCompleted.isEmpty()) {
            return "Elige una sola actividad de tu agenda y resérvale un bloque breve, concreto y sin interrupciones mañana."
        }
        return "Conserva el impulso: repite mañana una de las acciones que hoy sí estuvo alineada con la persona que eliges ser."
    }

    private fun buildJournalContent(
        draft: EveningReviewDraft,
        snapshot: EveningDaySnapshot,
        morningSession: MorningDialogSession?,
        suggestion: String
    ): String = buildString {
        appendLine("RESUMEN DE CIERRE")
        appendLine()
        appendLine("ENERGÍA")
        appendLine("${draft.energy}/5 — ${energyLabel(draft.energy)}")
        appendLine()
        appendLine("EMOCIÓN PREDOMINANTE")
        appendLine(emotionTitle(draft.predominantEmotionId))
        appendSection("LO QUE SALIÓ BIEN", draft.whatWentWell)
        appendSection("APRENDIZAJE", draft.learning)
        appendSection("PILOTO AUTOMÁTICO", draft.autopilotMoment)
        appendSection("GRATITUD", draft.gratitude)
        appendSection("PREPARADO PARA MAÑANA", draft.tomorrowPreparation)
        appendLine()
        appendLine("COHERENCIA CON MI IDENTIDAD")
        appendLine("${draft.identityAlignment}/5${morningSession?.identity?.takeIf(String::isNotBlank)?.let { " — $it" }.orEmpty()}")
        appendLine()
        appendLine("SÍNTESIS DEL DÍA")
        appendLine("Agenda completada (${snapshot.agendaCompleted.size}/${snapshot.agendaTotalCount}): ${snapshot.agendaCompleted.joinToString(" · ") { it.title }.ifBlank { "sin actividades marcadas" }}")
        appendLine("Metas: ${snapshot.completedGoalUnits.joinToString(" · ") { "${it.goalTitle} · ${it.unitName}" }.ifBlank { "sin unidades completadas" }}")
        appendLine("Presencia: ${snapshot.presenceReturns} regreso(s) al presente y ${snapshot.automaticPilotEvents} registro(s) de piloto automático.")
        appendLine("Coherencia: ${snapshot.coherenceAverageAfterScore?.let { "${snapshot.coherenceSessionsCount} sesión(es), bienestar final medio $it/10" } ?: "sin sesión registrada"}.")
        appendLine()
        appendLine("UNA MEJORA PARA MAÑANA")
        append(suggestion)
    }.trim()

    private fun StringBuilder.appendSection(title: String, value: String) {
        appendLine()
        appendLine(title)
        appendLine(value.trim().ifBlank { "Sin respuesta" })
    }

    companion object {
        fun energyLabel(value: Int) = listOf("Muy baja", "Baja", "Estable", "Buena", "Muy alta")[(value - 1).coerceIn(0, 4)]

        fun emotionTitle(id: String) = when (id) {
            "alegre" -> "Alegre"
            "ansioso" -> "Ansioso"
            "triste" -> "Triste"
            "enfadado" -> "Enfadado"
            "cansado" -> "Cansado"
            "agradecido" -> "Agradecido"
            else -> "Sereno"
        }

        private fun emotionEmoji(id: String) = when (id) {
            "alegre", "agradecido" -> "😊"
            "triste" -> "😢"
            "enfadado" -> "😠"
            "cansado" -> "😔"
            "ansioso" -> "😵‍💫"
            else -> "🤔"
        }

        private fun formatDay(dayStartMillis: Long): String =
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(dayStartMillis))
    }
}

private fun EveningReviewEntity.toDomain() = EveningReview(
    id, sessionDateEpochDay, completedAtEpochMillis, energy, predominantEmotionId,
    whatWentWell, learning, autopilotMoment, gratitude, tomorrowPreparation,
    identityAlignment, suggestion, agendaCompletedCount, agendaTotalCount,
    goalUnitsCompletedCount, presenceReturns, automaticPilotEvents,
    coherenceSessionsCount, dayContextFingerprint, journalEntryRequested, journalEntryCreated, journalEntryId
)

private fun EveningReview.toEntity() = EveningReviewEntity(
    id, sessionDateEpochDay, completedAtEpochMillis, energy, predominantEmotionId,
    whatWentWell, learning, autopilotMoment, gratitude, tomorrowPreparation,
    identityAlignment, suggestion, agendaCompletedCount, agendaTotalCount,
    goalUnitsCompletedCount, presenceReturns, automaticPilotEvents,
    coherenceSessionsCount, dayContextFingerprint, journalEntryRequested, journalEntryCreated, journalEntryId
)
