package iterative.harmony.backend.service

import iterative.harmony.backend.util.getLogger
import javax.sql.DataSource as JdbcDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ImportLock {
    private val log = getLogger()
    @Autowired private lateinit var jdbcDataSource: JdbcDataSource
    private val IMPORT_LOCK_KEY = 4_826_001L

    // Holds a Postgres lock during import. Skips if already locked
    fun runExclusive(codeBlock: () -> Unit) {
        jdbcDataSource.connection.use { connection ->
            val acquired =
                connection.prepareStatement("SELECT pg_try_advisory_lock(?)").use { statement ->
                    statement.setLong(1, IMPORT_LOCK_KEY)
                    statement.executeQuery().use { result -> result.next() && result.getBoolean(1) }
                }
            if (!acquired) {
                log.info("Import lock is held by another instance. Skipping run.")
                return
            }
            try {
                codeBlock()
            } finally {
                connection.prepareStatement("SELECT pg_advisory_unlock(?)").use { statement ->
                    statement.setLong(1, IMPORT_LOCK_KEY)
                    statement.execute()
                }
            }
        }
    }
}
