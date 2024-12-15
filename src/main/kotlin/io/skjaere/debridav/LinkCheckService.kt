package io.skjaere.debridav

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.http.HttpStatusCode
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class LinkCheckService(
    private val httpClient: HttpClient
) {

    @Suppress("SwallowedException")
    suspend fun isLinkAlive(link: String): Boolean {
        return try {
            httpClient.head(link).status == HttpStatusCode.OK
        } catch (e: IOException) {
            false
        }
    }
}
