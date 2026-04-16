package com.ypg.neville.feature.morningdialog.data

import com.ypg.neville.feature.morningdialog.domain.MorningDialogRepository
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMorningDialogRepository(
    private val dao: MorningDialogDao
) : MorningDialogRepository {

    override fun observeSessions(): Flow<List<MorningDialogSession>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getSession(sessionId: Long): MorningDialogSession? {
        return dao.getById(sessionId)?.toDomain()
    }

    override suspend fun getTodaySession(todayEpochDay: Long): MorningDialogSession? {
        return dao.getByDay(todayEpochDay)?.toDomain()
    }

    override suspend fun saveSession(session: MorningDialogSession) {
        dao.upsert(session.toEntity())
    }

    override suspend fun updateSessionNote(sessionId: Long, noteText: String) {
        dao.updateNote(sessionId, noteText.trim())
    }

    override suspend fun deleteSession(sessionId: Long) {
        dao.deleteById(sessionId)
    }
}

private fun MorningDialogSessionEntity.toDomain(): MorningDialogSession {
    return MorningDialogSession(
        id = id,
        sessionDateEpochDay = sessionDateEpochDay,
        completedAtEpochMillis = completedAtEpochMillis,
        goals = MorningDialogJsonCodec.decodeList(goalsJson),
        identity = identity,
        emotions = MorningDialogJsonCodec.decodeList(emotionsJson),
        anticipatedSituations = MorningDialogJsonCodec.decodeList(anticipatedSituationsJson),
        consciousResponses = MorningDialogJsonCodec.decodeList(consciousResponsesJson),
        noteText = noteText,
        completed = completed
    )
}

private fun MorningDialogSession.toEntity(): MorningDialogSessionEntity {
    return MorningDialogSessionEntity(
        id = id,
        sessionDateEpochDay = sessionDateEpochDay,
        completedAtEpochMillis = completedAtEpochMillis,
        goalsJson = MorningDialogJsonCodec.encodeList(goals),
        identity = identity,
        emotionsJson = MorningDialogJsonCodec.encodeList(emotions),
        anticipatedSituationsJson = MorningDialogJsonCodec.encodeList(anticipatedSituations),
        consciousResponsesJson = MorningDialogJsonCodec.encodeList(consciousResponses),
        noteText = noteText,
        completed = completed
    )
}
