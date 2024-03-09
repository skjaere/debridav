package io.william.debrid.qbittorrent

import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.core.io.ResourceLoader
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class QBittorrentEmulationController(
    private val torrentService: TorrentService,
    private val resourceLoader: ResourceLoader
) {
    @GetMapping("/api/v2/app/webapiVersion")
    fun version(): String {
        return "2.9.3"
    }

    @GetMapping("/api/v2/torrents/categories")
    fun categories(): Map<String, Category> {
        return torrentService.getCategories().associateBy { it.name!! }
    }

    @RequestMapping(path=  ["api/v2/torrents/createCategory"], method = [RequestMethod.POST], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun createCategory(@RequestParam category: String): Category {
        return torrentService.createCategory(category)
    }

    @GetMapping("api/v2/app/preferences")
    fun preferences(): String {
        return resourceLoader
            .getResource("classpath:qbittorrent_properties_response.json")
            .getContentAsString(Charsets.UTF_8)
    }


    @GetMapping("/version/api")
    fun versionTwo(): ResponseEntity<String> {
        return ResponseEntity.status(404).body("Not found")
    }

    @GetMapping("/api/v2/torrents/info")
    fun torrentsInfo(
        @RequestParam filter: String?,
        @RequestParam category: String?,
        @RequestParam tag: String?,
        @RequestParam sort: String?,
        @RequestParam reverse: Boolean?,
        @RequestParam limit: Int?,
        @RequestParam offset: Int?,
        @RequestParam hashes: String?
    ): List<TorrentsInfoResponse> {
        return torrentService
            .getTorrentsByCategory(category!!)
            .map {
                TorrentsInfoResponse.ofTorrent(it)
            }
    }

    @GetMapping("/api/v2/torrents/properties")
    fun torrentsProperties(@RequestParam hash: String): TorrentPropertiesResponse? {
        return torrentService.getTorrentByHash(hash)?.let {
            TorrentPropertiesResponse.ofTorrent(it)
        }
    }

    @GetMapping("/api/v2/torrents/files")
    fun torrentFiles(@RequestParam hash: String): List<TorrentFilesResponse>? {
        return torrentService.getTorrentByHash(hash)?.let {
            it.files?.map { torrentFile ->
                TorrentFilesResponse(
                    0,
                    torrentFile.fileName!!,
                    torrentFile.size!!.toInt(),
                    100,
                    1,
                    true,
                    pieceRange = listOf(1,100),
                    availability = 1.0.toFloat()
                )
            }
        }
    }

    @RequestMapping(path=  ["/api/v2/torrents/add"], method = [RequestMethod.POST], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addTorrent(
        @RequestPart urls: String,
        @RequestPart category: String,
        @RequestPart paused: String
    ): String {
        torrentService.addTorrent(category, urls)
        return "ok"
    }

    data class AddTorrentRequest(
        val urls: String,
        val torrents: ByteArray,
        val savepath: String?,
        val cookie: String,
        val category: String,
        val tags: String,
        @JsonAlias("skip_checking")
        val skipChecking: String,
        val paused: String,
        @JsonAlias("root_folder")
        val rootFolder: String?,
        val rename: String,
        val upLimit: Int,
        val dlLimit: Int,
        val ratioLimit: Float?,
        val seedingTimeLimit: Int?,
        @JsonAlias("autoTMM")
        val autoTmm: Boolean,
        val sequentialDownload: String?,
        val firstLastPiecePrio: String?
    )

    data class TorrentFilesResponse(
        val index: Int,
        val name: String,
        val size: Int,
        val progress: Int,
        val priority: Int,
        @JsonAlias("is_seed")
        val isSeed: Boolean,
        @JsonAlias("piece_range")
        val pieceRange: List<Int>,
        val availability: Float
    )
}