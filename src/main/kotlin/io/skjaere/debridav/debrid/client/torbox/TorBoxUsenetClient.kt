package io.skjaere.debridav.debrid.client.torbox

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.skjaere.debridav.debrid.client.DebridUsenetClient
import io.skjaere.debridav.debrid.client.torbox.model.usenet.CreateUsenetDownloadResponse
import io.skjaere.debridav.debrid.client.torbox.model.usenet.GetUsenetListItem
import io.skjaere.debridav.debrid.client.torbox.model.usenet.GetUsenetListResponse
import io.skjaere.debridav.fs.DebridProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
@ConditionalOnExpression("#{'\${debridav.debrid-clients}'.contains('torbox')}")
class TorBoxUsenetClient(
    private val httpClient: HttpClient,
    private val torBoxConfiguration: TorBoxConfiguration
) : DebridUsenetClient {

    override suspend fun addNzb(nzbFile: MultipartFile): CreateUsenetDownloadResponse {
        val resp = httpClient.post("${torBoxConfiguration.apiUrl}/api/usenet/createusenetdownload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        nzbFile.originalFilename?.let { append("name", it.substringBeforeLast(".")) }
                        append("file", nzbFile.bytes, Headers.build {
                            append(
                                HttpHeaders.ContentDisposition, "filename=${nzbFile.name}"
                            )
                            append(HttpHeaders.ContentType, "application/x-nzb")
                        })
                    },
                    boundary = "WebAppBoundary"
                )
            )
            headers {
                accept(ContentType.Application.Json)
                bearerAuth(torBoxConfiguration.apiKey)
            }
            timeout {
                requestTimeoutMillis = 20_000
            }
        }
        return resp.body<CreateUsenetDownloadResponse>()
    }

    override suspend fun getDownloads(ids: List<String>): List<GetUsenetListItem> = coroutineScope {
        ids.map {
            async { getDownloadInfo(it) }
        }.awaitAll()
    }

    override fun getProvider(): DebridProvider = DebridProvider.TORBOX

    private suspend fun getDownloadInfo(id: String): GetUsenetListItem {
        return httpClient.get("${torBoxConfiguration.apiUrl}/api/usenet/createusenetdownload/$id") {
            headers {
                accept(ContentType.Application.Json)
                bearerAuth(torBoxConfiguration.apiKey)
            }
        }.body<GetUsenetListResponse>().data
    }
}