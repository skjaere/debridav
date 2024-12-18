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
import io.skjaere.debridav.debrid.client.torbox.model.usenet.GetUsenetResponseListItemFile
import io.skjaere.debridav.debrid.client.torbox.model.usenet.RequestDLResponse
import io.skjaere.debridav.debrid.model.CachedFile
import io.skjaere.debridav.fs.DebridFileContents
import io.skjaere.debridav.fs.DebridProvider
import io.skjaere.debridav.fs.DebridTorrentFileContents
import io.skjaere.debridav.fs.DebridUsenetFileContents
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

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
                                HttpHeaders.ContentDisposition,
                                "filename=${nzbFile.originalFilename?.substringBeforeLast(".")}"
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
        return resp.body<CreateUsenetDownloadResponse>() //TODO: handle cached nzbs
    }

    override suspend fun getDownloads(ids: List<Int>): List<GetUsenetListItem> = coroutineScope {
        ids.map {
            async { getDownloadInfo(it) }
        }.awaitAll()
            .filterNotNull() // TODO: deal with missing download
    }

    override fun getProvider(): DebridProvider = DebridProvider.TORBOX

    suspend fun getCachedFiles(
        debridFileContents: DebridFileContents,
        params: Map<String, String>
    ): List<CachedFile> = coroutineScope {
        when (debridFileContents) {
            is DebridTorrentFileContents -> throw IllegalStateException("This client only supports debrid-usenet-files")
            is DebridUsenetFileContents -> getCachedFilesFromDownload(debridFileContents.usenetDownloadId)
        }
    }

    suspend fun getCachedFilesFromDownload(downloadId: Int): List<CachedFile> = coroutineScope {
        getDownloadInfo(downloadId)
            ?.files
            ?.map { remoteFile ->
                async {
                    getCachedFilesFromUsenetInfoListItem(remoteFile, downloadId)
                }
            }?.awaitAll() ?: emptyList()
    }

    suspend fun getCachedFilesFromUsenetInfoListItem(
        listItemFile: GetUsenetResponseListItemFile,
        downloadId: Int
    ): CachedFile = CachedFile(
        path = listItemFile.absolutePath,
        size = listItemFile.size,
        mimeType = listItemFile.mimetype,
        params = mapOf("fileId" to listItemFile.id, "downloadId" to downloadId.toString()),
        link = getStreamableLink(downloadId, listItemFile.id)!!,
        provider = DebridProvider.TORBOX,
        lastChecked = Instant.now().toEpochMilli()
    )


    override suspend fun getStreamableLink(downloadId: Int, fileId: String): String? {
        val resp = httpClient.get(
            "${torBoxConfiguration.apiUrl}/api/usenet/requestdl" +
                    "?token=${torBoxConfiguration.apiKey}" +
                    "&usenet_id=$downloadId" +
                    "&fileId=$fileId"
        ) {
            headers {
                accept(ContentType.Application.Json)
                bearerAuth(torBoxConfiguration.apiKey)
            }
        }
        return resp.body<RequestDLResponse>().data
    }

    private suspend fun getDownloadInfo(id: Int): GetUsenetListItem? {
        val resp = httpClient.get("${torBoxConfiguration.apiUrl}/api/usenet/mylist?id=$id") {
            headers {
                accept(ContentType.Application.Json)
                bearerAuth(torBoxConfiguration.apiKey)
            }
        }
        return resp.body<GetUsenetListResponse>().data // TODO: handle missing downloads
    }
}