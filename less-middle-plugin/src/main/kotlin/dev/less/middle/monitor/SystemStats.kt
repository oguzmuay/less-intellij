package dev.less.middle.monitor

import java.io.File
import java.lang.management.ManagementFactory
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import com.sun.management.OperatingSystemMXBean

/** Collects RAM, CPU, disk and IP readings; safe to call from a background thread. */
object SystemStats {
    private val os = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

    /** Takes one reading of all stats; individual failures fall back to zero or null. */
    fun read(): SystemSnapshot {
        val root = File("/")
        val (ip, leaked) = localIp()
        val (used, total) = memory()
        return SystemSnapshot(
            ramUsed = used,
            ramTotal = total,
            cpuLoad = os.cpuLoad.takeIf { it >= 0 } ?: 0.0,
            diskFree = root.usableSpace,
            diskTotal = root.totalSpace,
            ip = ip,
            ipLeaked = leaked,
        )
    }

    /**
     * Returns used and total physical memory; on macOS used = active + wired + compressor pages (vm_stat).
     * Example: 16 GB machine -> (9_400_000_000, 17_179_869_184)
     */
    private fun memory(): Pair<Long, Long> {
        val total = os.totalMemorySize
        val vm = runCommand("vm_stat") ?: return (total - os.freeMemorySize) to total
        val pageSize = Regex("page size of (\\d+) bytes").find(vm)?.groupValues?.get(1)?.toLongOrNull() ?: 16384L
        fun pages(label: String) = Regex("$label:\\s+(\\d+)").find(vm)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val used = (pages("Pages active") + pages("Pages wired down") + pages("Pages occupied by compressor")) * pageSize
        return if (used > 0) used to total else (total - os.freeMemorySize) to total
    }

    /**
     * Finds the primary local IPv4 and whether it is "leaked": a VPN tunnel with an IPv4 is up
     * while a physical interface still exposes its own address.
     * Example: en0 192.168.1.5 + utun4 10.8.0.2 -> ("192.168.1.5", true); only en0 -> ("192.168.1.5", false)
     */
    private fun localIp(): Pair<String?, Boolean> {
        val nics = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().filter { it.isUp && !it.isLoopback }
        fun ipv4(nic: NetworkInterface) = nic.inetAddresses.toList()
            .filterIsInstance<Inet4Address>().firstOrNull { !it.isLinkLocalAddress }?.hostAddress
        val physical = nics.filter { it.name.startsWith("en") }.firstNotNullOfOrNull { ipv4(it) }
        val tunnel = nics.filter { it.name.startsWith("utun") || it.name.startsWith("tun") || it.name.startsWith("ppp") }
            .firstNotNullOfOrNull { ipv4(it) }
        return physical to (physical != null && tunnel != null)
    }

    /** Runs a command with a 2s timeout and returns its stdout, or null on failure. */
    private fun runCommand(vararg cmd: String): String? = try {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        if (p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0) out else null
    } catch (e: Exception) {
        null
    }
}
