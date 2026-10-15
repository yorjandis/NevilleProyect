package com.ypg.neville.model.migration

import com.ypg.neville.feature.agenda.data.AgendaItemEntity
import com.ypg.neville.feature.calmspace.data.CalmPersonalPhraseEntity
import com.ypg.neville.feature.morningdialog.data.RitualDiaryExportEntity
import com.ypg.neville.model.db.room.ArchivedGoalEntity
import com.ypg.neville.model.db.room.ArchivedUnitEntity
import com.ypg.neville.model.db.room.DiarioAttachmentEntity
import com.ypg.neville.model.db.room.DiarioEntity
import com.ypg.neville.model.db.room.FraseEntity
import com.ypg.neville.model.db.room.GoalEntity
import com.ypg.neville.model.db.room.GoalUnitEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.NotaEntity
import com.ypg.neville.model.db.room.ReflexionEntity
import com.ypg.neville.model.db.room.SecureRoomText
import com.ypg.neville.model.security.PostQuantumAesTextCrypto
import org.json.JSONArray
import org.json.JSONObject

class NevilleMigrationRoomBridge(
    private val db: NevilleRoomDatabase
) {
    fun exportRecords(): List<CanonicalRecord> {
        return buildList {
            db.notaDao().getAll().map(SecureRoomText::decryptNota).forEach { add(it.toCanonical()) }
            db.diarioDao().getAll().map(SecureRoomText::decryptDiario).forEach { diary ->
                val parentRecord = diary.toCanonical()
                add(parentRecord)
                db.diarioAttachmentDao().getForDiario(diary.id).forEach { attachment ->
                    add(attachment.toCanonical(parentRecord))
                }
            }
            db.agendaItemDao().loadAll().forEach { add(it.toCanonical()) }
            db.goalDao().getAll().forEach { goal ->
                add(goal.toCanonical(db.goalUnitDao().getByGoalId(goal.id)))
            }
            db.archivedGoalDao().getAll().forEach { goal ->
                add(goal.toCanonical(db.archivedUnitDao().getByGoalId(goal.id)))
            }
            db.fraseDao().getPersonales().forEach { add(it.toCanonical()) }
            db.reflexionDao().getAll().forEach { add(it.toCanonical()) }
            db.ritualDiaryExportDao().getAll().forEach { add(it.toCanonical()) }
            db.calmPersonalPhraseDao().getAll().forEach { add(it.toCanonical()) }
        }
    }

    fun findConflicts(records: List<CanonicalRecord>): List<ImportConflict> {
        return records.mapNotNull { record ->
            when (record.type) {
                TYPE_NOTE -> detectAutoIdConflict(
                    record,
                    db.notaDao().getAll().map(SecureRoomText::decryptNota).map { it.toCanonical() }
                )
                TYPE_DIARY -> detectAutoIdConflict(
                    record,
                    db.diarioDao().getAll().map(SecureRoomText::decryptDiario).map { it.toCanonical() }
                )
                TYPE_DIARY_ATTACHMENT -> db.diarioAttachmentDao().getById(record.id)?.let { existing ->
                    val parent = db.diarioDao().getById(existing.diarioId)
                        ?.let(SecureRoomText::decryptDiario)
                        ?.toCanonical()
                        ?: return@let ImportConflict(record.type, record.id, "El anexo local no tiene una entrada asociada")
                    stableConflict(record, existing.toCanonical(parent))
                }
                TYPE_AGENDA -> db.agendaItemDao().findById(record.id)?.let {
                    stableConflict(record, it.toCanonical())
                }
                TYPE_GOAL -> db.goalDao().getById(record.id)?.let {
                    stableConflict(record, it.toCanonical(db.goalUnitDao().getByGoalId(it.id)))
                }
                TYPE_ARCHIVED_GOAL -> db.archivedGoalDao().getById(record.id)?.let {
                    stableConflict(record, it.toCanonical(db.archivedUnitDao().getByGoalId(it.id)))
                }
                TYPE_PERSONAL_PHRASE -> detectAutoIdConflict(record, db.fraseDao().getPersonales().map { it.toCanonical() })
                TYPE_PERSONAL_REFLECTION -> detectAutoIdConflict(record, db.reflexionDao().getAll().map { it.toCanonical() })
                TYPE_DAY_RITUAL_ARCHIVE -> detectRitualConflict(record)
                TYPE_CALM_PERSONAL_PHRASE -> detectAutoIdConflict(record, db.calmPersonalPhraseDao().getAll().map { it.toCanonical() })
                else -> ImportConflict(record.type, record.id, "Tipo no soportado por esta versión")
            }
        }
    }

    fun importRecords(
        records: List<CanonicalRecord>,
        policy: ImportPolicy = ImportPolicy.SkipExisting
    ): MigrationSummary {
        var summary = MigrationSummary()
        db.runInTransaction {
            val diaryRecords = records
                .filter { it.type == TYPE_DIARY }
                .associateBy { it.id }
            records.filter { it.type != TYPE_DIARY_ATTACHMENT }.forEach { record ->
                summary += importOne(record, policy)
            }
            records.filter { it.type == TYPE_DIARY_ATTACHMENT }.forEach { record ->
                val parentId = record.payload.optString("parentDiaryId")
                summary += importDiaryAttachment(
                    record = record,
                    parentRecord = diaryRecords[parentId],
                    policy = policy
                )
            }
        }
        return summary
    }

    private fun importOne(record: CanonicalRecord, policy: ImportPolicy): MigrationSummary {
        return when (record.type) {
            TYPE_NOTE -> {
                val stored = db.notaDao().getAll().firstOrNull {
                    SecureRoomText.decryptNota(it).toCanonical().id == record.id
                }
                val existing = stored?.let(SecureRoomText::decryptNota)
                importStableRecord(record, existing?.toCanonical(), policy) {
                    db.notaDao().insert(SecureRoomText.encryptNota(record.toNotaEntity(stored?.id ?: 0)))
                }
            }
            TYPE_DIARY -> {
                val stored = findStoredDiary(record)
                val existing = stored?.let(SecureRoomText::decryptDiario)
                importStableRecord(record, existing?.toCanonical(), policy) {
                    db.diarioDao().insert(SecureRoomText.encryptDiario(record.toDiarioEntity(stored?.id ?: 0)))
                }
            }
            TYPE_AGENDA -> importStableRecord(record, db.agendaItemDao().findById(record.id)?.toCanonical(), policy) {
                db.agendaItemDao().insert(record.toAgendaEntity())
            }
            TYPE_GOAL -> importStableRecord(
                record,
                db.goalDao().getById(record.id)?.let { it.toCanonical(db.goalUnitDao().getByGoalId(it.id)) },
                policy
            ) {
                db.goalDao().insert(record.toGoalEntity())
                db.goalUnitDao().deleteByGoalId(record.id)
                db.goalUnitDao().insertAll(record.toGoalUnits())
            }
            TYPE_ARCHIVED_GOAL -> importStableRecord(
                record,
                db.archivedGoalDao().getById(record.id)?.let { it.toCanonical(db.archivedUnitDao().getByGoalId(it.id)) },
                policy
            ) {
                db.archivedGoalDao().insert(record.toArchivedGoalEntity())
                db.archivedUnitDao().deleteByGoalId(record.id)
                db.archivedUnitDao().insertAll(record.toArchivedUnits())
            }
            TYPE_PERSONAL_PHRASE -> importAutoIdRecord(record, db.fraseDao().getPersonales().map { it.toCanonical() }) {
                db.fraseDao().insert(record.toFraseEntity())
            }
            TYPE_PERSONAL_REFLECTION -> importAutoIdRecord(record, db.reflexionDao().getAll().map { it.toCanonical() }) {
                db.reflexionDao().insert(record.toReflexionEntity())
            }
            TYPE_DAY_RITUAL_ARCHIVE -> importRitual(record)
            TYPE_CALM_PERSONAL_PHRASE -> importAutoIdRecord(record, db.calmPersonalPhraseDao().getAll().map { it.toCanonical() }) {
                db.calmPersonalPhraseDao().insert(record.toCalmPhraseEntity())
            }
            else -> MigrationSummary(skipped = 1, errors = 1)
        }
    }

    private fun importDiaryAttachment(
        record: CanonicalRecord,
        parentRecord: CanonicalRecord?,
        policy: ImportPolicy
    ): MigrationSummary {
        requireNotNull(parentRecord) { "El anexo no tiene una entrada del Diario asociada" }
        val diary = requireNotNull(findStoredDiary(parentRecord)) {
            "No se encontró la entrada del Diario asociada al anexo"
        }
        val clearData = MigrationFormat.decodeBase64(record.payload.getString("data"))
        require(record.payload.optLong("fileSize", -1L) == clearData.size.toLong()) {
            "El tamaño declarado del anexo no coincide"
        }
        val incomingDigest = record.payload.getString("sha256").lowercase()
        require(incomingDigest.isNotBlank() && MigrationIds.sha256Hex(clearData) == incomingDigest) {
            "La huella del anexo no coincide con su contenido"
        }

        val attachmentDao = db.diarioAttachmentDao()
        val existing = attachmentDao.getById(record.id)
        if (existing == null) {
            attachmentDao.upsert(record.toAttachmentEntity(diary.id, clearData))
            db.diarioDao().touch(
                diary.id,
                maxOf(diary.fechaM, MigrationFormat.isoToMillis(record.updatedAt))
            )
            return MigrationSummary(inserted = 1)
        }

        val isEquivalent = MigrationIds.sha256Hex(decryptedAttachmentData(existing)) == incomingDigest &&
            existing.fileName == record.payload.getString("fileName") &&
            existing.mimeType == record.payload.getString("mimeType") &&
            existing.diarioId == diary.id
        if (isEquivalent) return MigrationSummary(skipped = 1)

        val shouldReplace = when (policy) {
            ImportPolicy.SkipExisting -> return MigrationSummary(conflicts = 1)
            ImportPolicy.OverwriteExisting -> true
            ImportPolicy.NewerWins -> {
                val localUpdatedAt = existing.updatedAt
                val incomingUpdatedAt = MigrationFormat.isoToMillis(record.updatedAt)
                when {
                    incomingUpdatedAt > localUpdatedAt -> true
                    incomingUpdatedAt < localUpdatedAt -> return MigrationSummary(skipped = 1)
                    else -> return MigrationSummary(conflicts = 1)
                }
            }
            ImportPolicy.NewerWinsIncomingOnTie -> {
                val localUpdatedAt = existing.updatedAt
                val incomingUpdatedAt = MigrationFormat.isoToMillis(record.updatedAt)
                if (incomingUpdatedAt < localUpdatedAt) return MigrationSummary(skipped = 1)
                true
            }
            ImportPolicy.NewerWinsExistingOnTie -> {
                val localUpdatedAt = existing.updatedAt
                val incomingUpdatedAt = MigrationFormat.isoToMillis(record.updatedAt)
                if (incomingUpdatedAt <= localUpdatedAt) return MigrationSummary(skipped = 1)
                true
            }
        }

        if (shouldReplace) {
            attachmentDao.upsert(record.toAttachmentEntity(diary.id, clearData))
            db.diarioDao().touch(
                diary.id,
                maxOf(diary.fechaM, MigrationFormat.isoToMillis(record.updatedAt))
            )
        }
        return MigrationSummary(updated = 1)
    }

    private fun findStoredDiary(record: CanonicalRecord): DiarioEntity? {
        val stored = db.diarioDao().getAll()
        stored.firstOrNull { entity ->
            SecureRoomText.decryptDiario(entity).toCanonical().id == record.id
        }?.let { return it }

        val incomingKey = duplicateKey(record) ?: return null
        return stored.firstOrNull { entity ->
            duplicateKey(SecureRoomText.decryptDiario(entity).toCanonical()) == incomingKey
        }
    }

    private fun decryptedAttachmentData(attachment: DiarioAttachmentEntity): ByteArray {
        require(attachment.cryptoVersion == 1) { "Versión de cifrado de anexo no compatible" }
        return PostQuantumAesTextCrypto.decryptBytes(
            attachment.encryptedData,
            attachmentAad(attachment.id)
        )
    }

    private fun attachmentAad(id: String): String = "diary-attachment:$id"

    private fun importStableRecord(
        record: CanonicalRecord,
        existing: CanonicalRecord?,
        policy: ImportPolicy,
        insertOrReplace: () -> Unit
    ): MigrationSummary {
        if (existing == null) {
            insertOrReplace()
            return MigrationSummary(inserted = 1)
        }
        if (recordsEquivalent(existing, record)) return MigrationSummary(skipped = 1)
        return when (policy) {
            ImportPolicy.SkipExisting -> MigrationSummary(conflicts = 1)
            ImportPolicy.OverwriteExisting -> {
                insertOrReplace()
                MigrationSummary(updated = 1)
            }
            ImportPolicy.NewerWins -> when {
                MigrationFormat.isoToMillis(record.updatedAt) > MigrationFormat.isoToMillis(existing.updatedAt) -> {
                    insertOrReplace()
                    MigrationSummary(updated = 1)
                }
                MigrationFormat.isoToMillis(record.updatedAt) < MigrationFormat.isoToMillis(existing.updatedAt) -> MigrationSummary(skipped = 1)
                else -> MigrationSummary(conflicts = 1)
            }
            ImportPolicy.NewerWinsIncomingOnTie -> {
                val incomingUpdatedAt = MigrationFormat.isoToMillis(record.updatedAt)
                val existingUpdatedAt = MigrationFormat.isoToMillis(existing.updatedAt)
                if (incomingUpdatedAt >= existingUpdatedAt) {
                    insertOrReplace()
                    MigrationSummary(updated = 1)
                } else {
                    MigrationSummary(skipped = 1)
                }
            }
            ImportPolicy.NewerWinsExistingOnTie -> {
                val incomingUpdatedAt = MigrationFormat.isoToMillis(record.updatedAt)
                val existingUpdatedAt = MigrationFormat.isoToMillis(existing.updatedAt)
                if (incomingUpdatedAt > existingUpdatedAt) {
                    insertOrReplace()
                    MigrationSummary(updated = 1)
                } else {
                    MigrationSummary(skipped = 1)
                }
            }
        }
    }

    private fun importAutoIdRecord(
        record: CanonicalRecord,
        existingRecords: List<CanonicalRecord>,
        insert: () -> Unit
    ): MigrationSummary {
        val conflict = detectAutoIdConflict(record, existingRecords)
        if (conflict != null) {
            return if (existingRecords.any { recordsEquivalent(it, record) }) {
                MigrationSummary(skipped = 1)
            } else {
                MigrationSummary(conflicts = 1)
            }
        }
        insert()
        return MigrationSummary(inserted = 1)
    }

    private fun detectAutoIdConflict(
        incoming: CanonicalRecord,
        existingRecords: List<CanonicalRecord>
    ): ImportConflict? {
        val incomingDuplicateKey = duplicateKey(incoming)
        val match = existingRecords.firstOrNull {
            val existingDuplicateKey = duplicateKey(it)
            if (incomingDuplicateKey != null && existingDuplicateKey != null) {
                existingDuplicateKey == incomingDuplicateKey
            } else {
                it.id == incoming.id || it.contentFingerprint() == incoming.contentFingerprint()
            }
        } ?: return null
        return if (recordsEquivalent(match, incoming)) {
            ImportConflict(incoming.type, incoming.id, "Ya existe un elemento idéntico; se omitirá")
        } else {
            ImportConflict(incoming.type, incoming.id, "Duplicado por contenido y fecha con datos distintos")
        }
    }

    private fun duplicateKey(record: CanonicalRecord): String? {
        return when (record.type) {
            TYPE_NOTE -> duplicateKey(record.type, record.payload.optString("title"), record.payload.optString("body"))
            TYPE_DIARY -> duplicateKey(record.type, record.payload.optString("title"), record.payload.optString("body"))
            TYPE_PERSONAL_PHRASE -> duplicateKey(record.type, record.payload.optString("phrase"))
            else -> null
        }
    }

    private fun duplicateKey(type: String, vararg parts: String): String {
        return buildString {
            append(type)
            parts.forEach { part ->
                append('|')
                append(part.trim())
            }
        }
    }

    private fun stableConflict(incoming: CanonicalRecord, existing: CanonicalRecord): ImportConflict {
        return if (recordsEquivalent(existing, incoming)) {
            ImportConflict(incoming.type, incoming.id, "Ya existe un elemento idéntico; se omitirá")
        } else {
            ImportConflict(incoming.type, incoming.id, "Mismo ID estable con contenido distinto")
        }
    }

    private fun detectRitualConflict(record: CanonicalRecord): ImportConflict? {
        val localId = record.payload.optLong("sessionId", MigrationIds.stableLongId(record.type, record.id))
        val existing = db.ritualDiaryExportDao().getBySessionId(localId) ?: return null
        return stableConflict(record, existing.toCanonical())
    }

    private fun importRitual(record: CanonicalRecord): MigrationSummary {
        val conflict = detectRitualConflict(record)
        if (conflict != null) {
            val existing = db.ritualDiaryExportDao()
                .getBySessionId(record.payload.optLong("sessionId", MigrationIds.stableLongId(record.type, record.id)))
                ?.toCanonical()
            return if (existing != null && recordsEquivalent(existing, record)) {
                MigrationSummary(skipped = 1)
            } else {
                MigrationSummary(conflicts = 1)
            }
        }
        db.ritualDiaryExportDao().insert(record.toRitualEntity())
        return MigrationSummary(inserted = 1)
    }

    private fun recordsEquivalent(left: CanonicalRecord, right: CanonicalRecord): Boolean {
        if (left.type != right.type) return false
        if (left.type == TYPE_DIARY_ATTACHMENT) {
            return left.id == right.id &&
                left.payload.optString("sha256") == right.payload.optString("sha256") &&
                left.payload.optString("parentDiaryId") == right.payload.optString("parentDiaryId") &&
                left.payload.optString("fileName") == right.payload.optString("fileName") &&
                left.payload.optString("mimeType") == right.payload.optString("mimeType")
        }
        if (left.id == right.id) {
            return left.createdAt == right.createdAt &&
                left.updatedAt == right.updatedAt &&
                left.payload.toString() == right.payload.toString()
        }
        val leftKey = duplicateKey(left)
        val rightKey = duplicateKey(right)
        return leftKey != null && leftKey == rightKey &&
            left.createdAt == right.createdAt &&
            left.payload.toString() == right.payload.toString()
    }

    private fun NotaEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(fechaCreacion)
        val updated = MigrationFormat.millisToIso(fechaModificacion)
        val payload = JSONObject()
            .put("title", titulo)
            .put("body", nota)
            .put("tags", JSONArray().put(categoria).takeIf { categoria.isNotBlank() } ?: JSONArray())
            .put("category", categoria)
            .put("favorite", isFav)
            .put("isChecklist", isChecklist)
            .put("checklistJson", checklistJson)
        val canonicalId = syncId.ifBlank { MigrationIds.stableId(TYPE_NOTE, "android", id) }
        return CanonicalRecord(TYPE_NOTE, canonicalId, created, updated, payload = payload)
    }

    private fun DiarioEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(fecha)
        val updated = MigrationFormat.millisToIso(fechaM)
        val payload = JSONObject()
            .put("title", title)
            .put("body", content)
            .put("emotion", emocion)
            .put("chapter", capitulo)
            .put("favorite", isFav)
        val canonicalId = syncId.ifBlank { MigrationIds.stableId(TYPE_DIARY, "android", id) }
        return CanonicalRecord(TYPE_DIARY, canonicalId, created, updated, payload = payload)
    }

    private fun DiarioAttachmentEntity.toCanonical(parentRecord: CanonicalRecord): CanonicalRecord {
        val clearData = decryptedAttachmentData(this)
        val payload = JSONObject()
            .put("parentDiaryId", parentRecord.id)
            .put("fileName", fileName)
            .put("mimeType", mimeType)
            .put("fileSize", clearData.size.toLong())
            .put("sha256", MigrationIds.sha256Hex(clearData))
            .put("data", MigrationFormat.encodeBase64(clearData))
        return CanonicalRecord(
            type = TYPE_DIARY_ATTACHMENT,
            id = id,
            createdAt = MigrationFormat.millisToIso(createdAt),
            updatedAt = MigrationFormat.millisToIso(updatedAt),
            payload = payload
        )
    }

    private fun AgendaItemEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(createdAt)
        val updated = MigrationFormat.millisToIso(updatedAt)
        val payload = JSONObject()
            .put("title", title)
            .put("note", note)
            .put("activityDateMillis", activityDateMillis)
            .put("activityTimeMillis", activityTimeMillis)
            .put("place", place)
            .put("content", content)
            .put("priority", priority)
            .put("colorHex", colorHex)
            .put("completed", completed)
            .put("reminderActive", false)
            .put("reminderId", JSONObject.NULL)
        return CanonicalRecord(TYPE_AGENDA, id, created, updated, payload = payload)
    }

    private fun GoalEntity.toCanonical(units: List<GoalUnitEntity>): CanonicalRecord {
        val createdMillis = startDate ?: 0L
        val updatedMillis = units.mapNotNull { it.completedDate ?: it.endDate ?: it.startDate }.maxOrNull() ?: createdMillis
        val payload = JSONObject()
            .put("title", title)
            .put("descriptionText", descriptionText)
            .put("totalUnits", totalUnits)
            .put("unitType", unitType)
            .put("frequency", frequency)
            .put("scheduleType", scheduleType)
            .put("weeklyDaysPerWeek", weeklyDaysPerWeek)
            .put("dayPeriod", dayPeriod)
            .put("customUnitLabel", customUnitLabel)
            .put("executionTargetValue", executionTargetValue)
            .put("completionBasis", completionBasis)
            .put("durationValue", durationValue)
            .put("durationUnit", durationUnit)
            .put("isStarted", isStarted)
            .put("startDate", startDate)
            .put("notifyOnUnitAvailable", notifyOnUnitAvailable)
            .put("lastNotifiedUnitIndex", lastNotifiedUnitIndex)
            .put("status", if (units.isNotEmpty() && units.all { it.status == "completed" }) "completed" else "active")
            .put("units", JSONArray().also { array -> units.forEach { array.put(it.toJson()) } })
        return CanonicalRecord(
            TYPE_GOAL,
            id,
            MigrationFormat.millisToIso(createdMillis),
            MigrationFormat.millisToIso(updatedMillis),
            payload = payload
        )
    }

    private fun ArchivedGoalEntity.toCanonical(units: List<ArchivedUnitEntity>): CanonicalRecord {
        val created = MigrationFormat.millisToIso(completionDate)
        val payload = JSONObject()
            .put("title", title)
            .put("descriptionText", descriptionText)
            .put("totalUnits", totalUnits)
            .put("unitType", unitType)
            .put("frequency", frequency)
            .put("scheduleType", scheduleType)
            .put("weeklyDaysPerWeek", weeklyDaysPerWeek)
            .put("dayPeriod", dayPeriod)
            .put("customUnitLabel", customUnitLabel)
            .put("executionTargetValue", executionTargetValue)
            .put("completionBasis", completionBasis)
            .put("durationValue", durationValue)
            .put("durationUnit", durationUnit)
            .put("completionDate", completionDate)
            .put("status", "archived")
            .put("units", JSONArray().also { array -> units.forEach { array.put(it.toJson()) } })
        return CanonicalRecord(TYPE_ARCHIVED_GOAL, id, created, created, payload = payload)
    }

    private fun FraseEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(0L)
        val payload = JSONObject()
            .put("phrase", frase)
            .put("author", autor)
            .put("source", fuente)
            .put("favorite", favState() == "1")
            .put("note", nota)
            .put("category", categoria)
        return CanonicalRecord(TYPE_PERSONAL_PHRASE, MigrationIds.stableId(TYPE_PERSONAL_PHRASE, frase, autor), created, created, payload = payload)
    }

    private fun ReflexionEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(fechaCreacion)
        val updated = MigrationFormat.millisToIso(fechaModificacion)
        val payload = JSONObject()
            .put("title", titulo)
            .put("content", contenido)
            .put("favorite", favorito)
            .put("note", nota)
        return CanonicalRecord(TYPE_PERSONAL_REFLECTION, MigrationIds.stableId(TYPE_PERSONAL_REFLECTION, created, titulo, contenido), created, updated, payload = payload)
    }

    private fun RitualDiaryExportEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(createdAt)
        val payload = JSONObject()
            .put("sessionId", sessionId)
            .put("diarioId", diarioId)
            .put("createdAtMillis", createdAt)
        return CanonicalRecord(TYPE_DAY_RITUAL_ARCHIVE, MigrationIds.stableId(TYPE_DAY_RITUAL_ARCHIVE, sessionId, diarioId, createdAt), created, created, payload = payload)
    }

    private fun CalmPersonalPhraseEntity.toCanonical(): CanonicalRecord {
        val created = MigrationFormat.millisToIso(createdAt)
        val updated = MigrationFormat.millisToIso(updatedAt)
        val payload = JSONObject().put("phrase", phrase)
        return CanonicalRecord(TYPE_CALM_PERSONAL_PHRASE, MigrationIds.stableId(TYPE_CALM_PERSONAL_PHRASE, phrase, created), created, updated, payload = payload)
    }

    private fun GoalUnitEntity.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("goalId", goalId)
            .put("unitIndex", unitIndex)
            .put("status", status)
            .put("unitType", unitType)
            .put("name", name)
            .put("info", info)
            .put("note", note)
            .put("startDate", startDate)
            .put("endDate", endDate)
            .put("completedDate", completedDate)
    }

    private fun ArchivedUnitEntity.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("goalId", goalId)
            .put("unitIndex", unitIndex)
            .put("status", status)
            .put("name", name)
            .put("info", info)
            .put("note", note)
            .put("startDate", startDate)
            .put("endDate", endDate)
            .put("completedDate", completedDate)
    }

    private fun CanonicalRecord.toNotaEntity(localId: Long = 0): NotaEntity {
        return NotaEntity(
            id = localId,
            titulo = payload.getString("title"),
            nota = payload.getString("body"),
            fechaCreacion = MigrationFormat.isoToMillis(createdAt),
            fechaModificacion = MigrationFormat.isoToMillis(updatedAt),
            isFav = payload.optBoolean("favorite", false),
            categoria = payload.optString("category", ""),
            isChecklist = payload.optBoolean("isChecklist", false),
            checklistJson = payload.optString("checklistJson", ""),
            syncId = id
        )
    }

    private fun CanonicalRecord.toDiarioEntity(localId: Long = 0): DiarioEntity {
        return DiarioEntity(
            id = localId,
            content = payload.getString("body"),
            title = payload.getString("title"),
            emocion = payload.optString("emotion", "\uD83D\uDE0C"),
            capitulo = payload.optString("chapter", ""),
            fecha = MigrationFormat.isoToMillis(createdAt),
            fechaM = MigrationFormat.isoToMillis(updatedAt),
            isFav = payload.optBoolean("favorite", false),
            syncId = id
        )
    }

    private fun CanonicalRecord.toAttachmentEntity(
        diarioId: Long,
        clearData: ByteArray
    ): DiarioAttachmentEntity {
        return DiarioAttachmentEntity(
            id = id,
            diarioId = diarioId,
            fileName = payload.getString("fileName"),
            mimeType = payload.optString("mimeType", "application/octet-stream"),
            fileSize = clearData.size.toLong(),
            createdAt = MigrationFormat.isoToMillis(createdAt),
            updatedAt = MigrationFormat.isoToMillis(updatedAt),
            cryptoVersion = 1,
            encryptedData = PostQuantumAesTextCrypto.encryptBytes(clearData, attachmentAad(id))
        )
    }

    private fun CanonicalRecord.toAgendaEntity(): AgendaItemEntity {
        return AgendaItemEntity(
            id = id,
            title = payload.getString("title"),
            createdAt = MigrationFormat.isoToMillis(createdAt),
            updatedAt = MigrationFormat.isoToMillis(updatedAt),
            note = payload.optString("note", ""),
            activityDateMillis = payload.optLong("activityDateMillis", 0L),
            activityTimeMillis = payload.optLong("activityTimeMillis", 0L),
            place = payload.optString("place", ""),
            content = payload.optString("content", ""),
            priority = payload.optString("priority", ""),
            colorHex = payload.optString("colorHex", ""),
            completed = if (payload.isNull("completed")) null else payload.optBoolean("completed"),
            reminderActive = false,
            reminderId = null
        )
    }

    private fun CanonicalRecord.toGoalEntity(): GoalEntity {
        return GoalEntity(
            id = id,
            title = payload.getString("title"),
            descriptionText = payload.optString("descriptionText", ""),
            totalUnits = payload.optInt("totalUnits", 0),
            unitType = payload.optString("unitType", ""),
            frequency = payload.optInt("frequency", 0),
            scheduleType = payload.optString("scheduleType", "interval"),
            weeklyDaysPerWeek = payload.optInt("weeklyDaysPerWeek", 3),
            dayPeriod = payload.optString("dayPeriod", "anytime"),
            customUnitLabel = payload.optString("customUnitLabel", ""),
            executionTargetValue = payload.optDouble("executionTargetValue", 1.0),
            completionBasis = payload.optString("completionBasis", "executions"),
            durationValue = payload.optInt("durationValue", 0),
            durationUnit = payload.optString("durationUnit", "dias"),
            isStarted = payload.optBoolean("isStarted", false),
            startDate = payload.optNullableLong("startDate"),
            notifyOnUnitAvailable = payload.optBoolean("notifyOnUnitAvailable", false),
            lastNotifiedUnitIndex = payload.optInt("lastNotifiedUnitIndex", 0)
        )
    }

    private fun CanonicalRecord.toGoalUnits(): List<GoalUnitEntity> {
        val units = payload.optJSONArray("units") ?: JSONArray()
        return (0 until units.length()).map { index ->
            val item = units.getJSONObject(index)
            GoalUnitEntity(
                id = item.getString("id"),
                goalId = id,
                unitIndex = item.optInt("unitIndex", index),
                status = item.optString("status", "pending"),
                unitType = item.optString("unitType", payload.optString("unitType", "")),
                name = item.optString("name", ""),
                info = item.optString("info", ""),
                note = item.optString("note", ""),
                startDate = item.optNullableLong("startDate"),
                endDate = item.optNullableLong("endDate"),
                completedDate = item.optNullableLong("completedDate")
            )
        }
    }

    private fun CanonicalRecord.toArchivedGoalEntity(): ArchivedGoalEntity {
        return ArchivedGoalEntity(
            id = id,
            title = payload.getString("title"),
            descriptionText = payload.optString("descriptionText", ""),
            totalUnits = payload.optInt("totalUnits", 0),
            unitType = payload.optString("unitType", ""),
            frequency = payload.optInt("frequency", 0),
            scheduleType = payload.optString("scheduleType", "interval"),
            weeklyDaysPerWeek = payload.optInt("weeklyDaysPerWeek", 3),
            dayPeriod = payload.optString("dayPeriod", "anytime"),
            customUnitLabel = payload.optString("customUnitLabel", ""),
            executionTargetValue = payload.optDouble("executionTargetValue", 1.0),
            completionBasis = payload.optString("completionBasis", "executions"),
            durationValue = payload.optInt("durationValue", 0),
            durationUnit = payload.optString("durationUnit", "dias"),
            completionDate = payload.optLong("completionDate", MigrationFormat.isoToMillis(updatedAt))
        )
    }

    private fun CanonicalRecord.toArchivedUnits(): List<ArchivedUnitEntity> {
        val units = payload.optJSONArray("units") ?: JSONArray()
        return (0 until units.length()).map { index ->
            val item = units.getJSONObject(index)
            ArchivedUnitEntity(
                id = item.getString("id"),
                goalId = id,
                unitIndex = item.optInt("unitIndex", index),
                status = item.optString("status", "completed"),
                name = item.optString("name", ""),
                info = item.optString("info", ""),
                note = item.optString("note", ""),
                startDate = item.optNullableLong("startDate"),
                endDate = item.optNullableLong("endDate"),
                completedDate = item.optNullableLong("completedDate")
            )
        }
    }

    private fun CanonicalRecord.toFraseEntity(): FraseEntity {
        return FraseEntity(
            frase = payload.getString("phrase"),
            autor = payload.optString("author", ""),
            fuente = payload.optString("source", ""),
            isfav = if (payload.optBoolean("favorite", false)) "1" else "0",
            personal = "1",
            fav = if (payload.optBoolean("favorite", false)) "1" else "0",
            nota = payload.optString("note", ""),
            inbuild = "0",
            categoria = payload.optString("category", "OTROS")
        )
    }

    private fun CanonicalRecord.toReflexionEntity(): ReflexionEntity {
        return ReflexionEntity(
            titulo = payload.getString("title"),
            contenido = payload.getString("content"),
            favorito = payload.optBoolean("favorite", false),
            nota = payload.optString("note", ""),
            fechaCreacion = MigrationFormat.isoToMillis(createdAt),
            fechaModificacion = MigrationFormat.isoToMillis(updatedAt)
        )
    }

    private fun CanonicalRecord.toRitualEntity(): RitualDiaryExportEntity {
        return RitualDiaryExportEntity(
            sessionId = payload.optLong("sessionId", MigrationIds.stableLongId(type, id)),
            diarioId = payload.optLong("diarioId", 0L),
            createdAt = payload.optLong("createdAtMillis", MigrationFormat.isoToMillis(createdAt))
        )
    }

    private fun CanonicalRecord.toCalmPhraseEntity(): CalmPersonalPhraseEntity {
        return CalmPersonalPhraseEntity(
            phrase = payload.getString("phrase"),
            createdAt = MigrationFormat.isoToMillis(createdAt),
            updatedAt = MigrationFormat.isoToMillis(updatedAt)
        )
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (!has(key) || isNull(key)) null else optLong(key)
    }

    companion object {
        const val TYPE_NOTE = "note"
        const val TYPE_DIARY = "diary_entry"
        const val TYPE_DIARY_ATTACHMENT = "diary_attachment"
        const val TYPE_AGENDA = "agenda_entry"
        const val TYPE_GOAL = "goal"
        const val TYPE_ARCHIVED_GOAL = "archived_goal"
        const val TYPE_PERSONAL_PHRASE = "personal_phrase"
        const val TYPE_PERSONAL_REFLECTION = "personal_reflection"
        const val TYPE_DAY_RITUAL_ARCHIVE = "day_ritual_archive"
        const val TYPE_CALM_PERSONAL_PHRASE = "calm_personal_phrase"
    }
}
