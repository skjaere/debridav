package io.william.debridav.debrid.realdebrid

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.william.debridav.debrid.CachedFile
import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.realdebrid.model.HashResponse
import io.william.debridav.debrid.realdebrid.model.TorrentsInfo
import io.william.debridav.fs.DebridProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import java.nio.channels.UnresolvedAddressException
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap


@Component
@ConditionalOnExpression("#{'\${debridav.debrid-clients}'.contains('real_debrid')}")
class RealDebridClient(
    private val realDebridConfiguration: RealDebridConfiguration,
    private val httpClient: HttpClient
) : DebridClient {
    private val logger = LoggerFactory.getLogger(RealDebridClient::class.java)

    override suspend fun isCached(magnet: String): Boolean = coroutineScope {
        try {
            val hash = MagnetParser.getHashFromMagnet(magnet)?.lowercase(Locale.getDefault())
            val resp = httpClient
                .get("${realDebridConfiguration.baseUrl}/torrents/instantAvailability/$hash") {
                    headers {
                        set(HttpHeaders.Accept, "application/json")
                        set(HttpHeaders.Authorization, "Bearer ${realDebridConfiguration.apiKey}")
                    }
                }.body<HashMap<String, HashResponse>>()
            resp[hash]?.get("rd")
                ?.toList()
                ?.isNotEmpty() ?: false
        } catch (e: UnresolvedAddressException) {
            logger.error("Failed to check cache for $magnet")
            throw RuntimeException(e)
        } catch (e: Exception) {
            logger.error("something went wrong", e)
            throw RuntimeException(e)
        }
    }

    override suspend fun getCachedFiles(magnet: String, params: Map<String, String>): List<CachedFile> {
        if (params.containsKey("torrentId")) {
            val torrentId = params["torrentId"]!!
            return getCachedFilesFromTorrentId(torrentId)
        } else {
            logger.info("getting cached files from real debrid")
            return addMagnet(magnet)?.let { addMagnetResponse ->
                getCachedFilesFromTorrentId(addMagnetResponse.id)
            } ?: emptyList()
        }
    }

    private suspend fun getCachedFilesFromTorrentId(torrentId: String): List<CachedFile> {
        val movieExtensions = listOf(".mkv", ".mp4")
        return getCachedFilesFromTorrent(
            getAllFilesInTorrent(torrentId),
            movieExtensions,
            torrentId
        )
    }

    private suspend fun getCachedFilesFromTorrent(
        torrentInfo: Torrent,
        movieExtensions: List<String>,
        torrentId: String
    ): List<CachedFile> = coroutineScope{
        val filmFileIds = torrentInfo.files
            .filter { hostedFile -> movieExtensions.any { extension -> hostedFile.fileName.endsWith(extension) } }
            .map { hostedFile -> hostedFile.fileId }
        selectFilesFromTorrent(torrentId, filmFileIds)
        getTorrentInfoSelected(torrentId).let { selectedHostedFiles ->
            selectedHostedFiles
                .map { async { unrestrictLink(it.link!!) } }
                .awaitAll()
                .map { unrestrictedLink ->
                    logger.info("done getting cached files from real debrid")
                    CachedFile(
                        "${torrentInfo.name}/${unrestrictedLink.filename}",
                        unrestrictedLink.filesize,
                        unrestrictedLink.mimeType,
                        unrestrictedLink.download,
                        DebridProvider.REAL_DEBRID,
                        Instant.now().toEpochMilli(),
                        mapOf(
                            "torrentId" to torrentId,
                            "fileId" to unrestrictedLink.id
                        )
                    )
                }
        }
    }

    override fun getProvider(): DebridProvider = DebridProvider.REAL_DEBRID

    @Serializable
    data class AddMagnetResponse(
        val id: String,
        val uri: String
    )

    private suspend fun addMagnet(magnet: String): AddMagnetResponse? {
        return httpClient
            .post("${realDebridConfiguration.baseUrl}/torrents/addMagnet") {
                headers {
                    set(HttpHeaders.Accept, "application/json")
                    set(HttpHeaders.Authorization, "Bearer ${realDebridConfiguration.apiKey}")
                    set(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                }
                setBody("magnet=$magnet")
            }.body<AddMagnetResponse>()
    }

    data class Torrent(
        val id: String,
        val name: String,
        val files: List<HostedFile>
    )

    data class HostedFile(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val link: String?
    )

    private suspend fun getTorrentInfo(id: String): TorrentsInfo {
        return httpClient
            .get("${realDebridConfiguration.baseUrl}/torrents/info/$id") {
                headers {
                    set(HttpHeaders.Accept, "application/json")
                    set(HttpHeaders.Authorization, "Bearer ${realDebridConfiguration.apiKey}")
                }
            }.body<TorrentsInfo>()
    }

    private suspend fun getAllFilesInTorrent(id: String): Torrent {
        return getTorrentInfo(id)
            .let {
                Torrent(
                    it.id,
                    it.filename,
                    it.files.map { file ->
                        HostedFile(
                            file.id.toString(),
                            file.path,
                            file.bytes,
                            null
                        )
                    }
                )
            }
    }

    private suspend fun getTorrentInfoSelected(id: String): List<HostedFile> {
        return getTorrentInfo(id)
            .let { torrentInfo ->
                torrentInfo.files
                    .filter { file -> file.selected == 1 }
                    .mapIndexed { idx, file ->
                        HostedFile(
                            file.id.toString(),
                            file.path,
                            file.bytes,
                            torrentInfo.links[idx]
                        )
                    }
            }
    }

    private suspend fun selectFilesFromTorrent(torrentId: String, fileIds: List<String>) {
        val status: Int = httpClient
            .post("${realDebridConfiguration.baseUrl}/torrents/selectFiles/$torrentId") {
                headers {
                    set(HttpHeaders.Accept, "application/json")
                    set(HttpHeaders.Authorization, "Bearer ${realDebridConfiguration.apiKey}")
                    set(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                }
                setBody("files=${fileIds.joinToString(",")}")
            }.status.value
        if (status in 400..404) {
            throw RuntimeException("error selecting files. Response code: $status")
        }
    }


    @Serializable
    data class UnrestrictedLink(
        val id: String,
        val filename: String,
        val mimeType: String,
        val filesize: Long,
        val link: String,
        val host: String,
        val download: String
    )

    private suspend fun unrestrictLink(link: String): UnrestrictedLink = coroutineScope {
        httpClient
            .post("${realDebridConfiguration.baseUrl}/unrestrict/link") {
                headers {
                    set(HttpHeaders.Accept, "application/json")
                    set(HttpHeaders.Authorization, "Bearer ${realDebridConfiguration.apiKey}")
                    set(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                }
                setBody("link=$link")
            }.body<UnrestrictedLink>()
    }
}