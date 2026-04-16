package com.ypg.neville.model.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BackupRestoreSignal {
    private val _restoreTick = MutableStateFlow(0L)
    val restoreTick: StateFlow<Long> = _restoreTick.asStateFlow()

    fun notifyDataRestored() {
        _restoreTick.value = System.currentTimeMillis()
    }
}
