package com.ypg.neville.feature.morningdialog.domain

import kotlinx.coroutines.flow.Flow

interface MorningDialogRepository {
    fun observeSessions(): Flow<List<MorningDialogSession>>
    suspend fun getSession(sessionId: Long): MorningDialogSession?
    suspend fun getTodaySession(todayEpochDay: Long): MorningDialogSession?
    suspend fun saveSession(session: MorningDialogSession)
    suspend fun updateSessionNote(sessionId: Long, noteText: String)
    suspend fun deleteSession(sessionId: Long)
}
