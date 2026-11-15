package dev.less.middle.monitor

/** One reading of machine stats shown in the status bar; sizes are in bytes, cpu is 0.0-1.0. */
data class SystemSnapshot(
    val ramUsed: Long,
    val ramTotal: Long,
    val cpuLoad: Double,
    val diskFree: Long,
    val diskTotal: Long,
    val ip: String?,
    val ipLeaked: Boolean,
)
