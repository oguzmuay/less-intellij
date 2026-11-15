package dev.less.middle.monitor

import java.util.Locale

/** Formats a [SystemSnapshot] as the status bar text. */
object SystemMonitorFormat {
    /**
     * Formats bytes as GB with one decimal.
     * Example: 17_179_869_184 -> "16.0G"
     */
    fun gb(bytes: Long): String = String.format(Locale.US, "%.1fG", bytes / 1024.0 / 1024.0 / 1024.0)

    /**
     * Builds the one-line status text.
     * Example: -> "RAM 9.4/16.0G  CPU 23%  Disk 120.5G free  IP 192.168.1.5"; leaked -> "...  Leaked IP 192.168.1.5"
     */
    fun text(s: SystemSnapshot): String {
        val ip = s.ip?.let { "  ${if (s.ipLeaked) "Leaked IP" else "IP"} $it" }.orEmpty()
        return "RAM ${gb(s.ramUsed)}/${gb(s.ramTotal)}  CPU ${(s.cpuLoad * 100).toInt()}%  Disk ${gb(s.diskFree)} free$ip"
    }
}
