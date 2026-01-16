package iterative.harmony.backend.service

import iterative.harmony.backend.util.CacheConstants.TEAM_AVAILABILITY_BY_ID
import iterative.harmony.backend.util.CacheConstants.USER_AVAILABILITY_BY_ID
import iterative.harmony.backend.util.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class CacheService {
    private val log = getLogger()
    @Autowired private lateinit var cacheManager: CacheManager

    private fun <K : Any> clearCache(name: String, key: K) {
        log.debug("Clearing {} cache for key: {}", name, key)
        cacheManager.getCache(name)?.evict(key)
    }

    fun clearUserAvailabilityCache(userId: String) {
        clearCache(USER_AVAILABILITY_BY_ID, userId)
    }

    fun clearTeamAvailabilityCache(teamId: Long) {
        clearCache(TEAM_AVAILABILITY_BY_ID, teamId)
    }
}
