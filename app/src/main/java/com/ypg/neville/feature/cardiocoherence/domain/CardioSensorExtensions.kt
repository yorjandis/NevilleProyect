package com.ypg.neville.feature.cardiocoherence.domain

data class CardioSignalSnapshot(
    val heartRateBpm: Int? = null,
    val hrvMillis: Double? = null,
    val source: String = SENSOR_SOURCE_MVP
)

interface CardioSignalProvider {
    suspend fun latestSnapshot(): CardioSignalSnapshot?
}

class NoOpCardioSignalProvider : CardioSignalProvider {
    override suspend fun latestSnapshot(): CardioSignalSnapshot? = null
}

const val SENSOR_SOURCE_MVP = "mvp_without_sensors"
const val SENSOR_SOURCE_HEALTH_CONNECT = "health_connect_future"
const val SENSOR_SOURCE_WEAR_OS = "wear_os_future"
const val SENSOR_SOURCE_HEALTHKIT_BRIDGE = "healthkit_bridge_future"
const val SENSOR_SOURCE_APPLE_WATCH_BRIDGE = "apple_watch_bridge_future"
