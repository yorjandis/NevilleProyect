package com.ypg.neville.model.wirelesssync

import com.ypg.neville.model.migration.MigrationSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class WirelessSyncProtocolTest {

    @Test
    fun importResult_roundTripsAllSummaryFields() {
        val result = WirelessImportResult.from(
            MigrationSummary(inserted = 3, updated = 2, skipped = 4, conflicts = 1, errors = 0),
            "complete"
        )

        assertEquals(result, WirelessImportResult.fromJson(result.toJsonBytes()))
    }

    @Test
    fun bidirectionalSummary_combinesBothImportResults() {
        val summary = WirelessTransferSummary(
            direction = WirelessSyncDirection.Bidirectional,
            sent = 8,
            received = 7,
            localImport = WirelessImportResult(2, 1, 4, 0, 0, "local"),
            remoteImport = WirelessImportResult(3, 2, 3, 0, 0, "remote")
        )

        assertEquals(5, summary.totalInserted)
        assertEquals(3, summary.totalUpdated)
        assertEquals(7, summary.totalSkipped)
        assertEquals(0, summary.totalConflicts)
        assertEquals(0, summary.totalErrors)
    }
}
