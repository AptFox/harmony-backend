package iterative.harmony.backend.config

import iterative.harmony.backend.util.getLogger
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.util.concurrent.TimeUnit
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JvmMemoryLogger(private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()) {
    private val log = getLogger()

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun logMemoryUsage() {
        val heap = memoryBean.heapMemoryUsage
        val nonHeap = memoryBean.nonHeapMemoryUsage

        log.info(
            "JVM memory | heap: used={}MB committed={}MB max={}MB | non-heap: used={}MB committed={}MB",
            toMb(heap.used),
            toMb(heap.committed),
            toMb(heap.max),
            toMb(nonHeap.used),
            toMb(nonHeap.committed),
        )
    }

    private fun toMb(bytes: Long): Long = bytes / 1024 / 1024
}
