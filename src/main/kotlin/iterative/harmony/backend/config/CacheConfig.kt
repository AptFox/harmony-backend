package iterative.harmony.backend.config

import com.github.benmanes.caffeine.cache.Caffeine
import iterative.harmony.backend.util.CacheConstants.FRANCHISE_TEAMS
import iterative.harmony.backend.util.CacheConstants.TEAM_AVAILABILITY_BY_ID
import iterative.harmony.backend.util.CacheConstants.USER_AVAILABILITY_BY_ID
import iterative.harmony.backend.util.CacheConstants.USER_BY_ID
import java.util.concurrent.TimeUnit
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val manager = SimpleCacheManager()
        manager.setCaches(
            listOf(
                buildCache(FRANCHISE_TEAMS),
                buildCache(TEAM_AVAILABILITY_BY_ID),
                buildCache(USER_AVAILABILITY_BY_ID),
                buildCache(USER_BY_ID),
            )
        )
        return manager
    }

    private fun buildCache(name: String, minutes: Long = 60, size: Long = 300): CaffeineCache {
        return CaffeineCache(
            name,
            Caffeine.newBuilder()
                .expireAfterWrite(minutes, TimeUnit.MINUTES)
                .maximumSize(size)
                .build(),
        )
    }
}
