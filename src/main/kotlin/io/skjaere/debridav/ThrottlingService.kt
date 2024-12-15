package io.skjaere.debridav

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service

@Service
class ThrottlingService {
    companion object {
        const val CACHE_SIZE = 10L
    }

    private val cache: LoadingCache<String, Mutex> = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE)
        .build(CacheLoader.from { _ -> Mutex() })

    suspend fun <T> throttle(key: String, delay: Long, block: suspend CoroutineScope.() -> T): T = coroutineScope {
        cache.get(key).withLock { delay(delay) }
        block.invoke(this)
    }
}
