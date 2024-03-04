package io.william.debrid.qbittorrent

import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class QBittorrentController {
    @GetMapping("/api/v2/app/version")
    fun version(): String = "v0.0.1"

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
    ): TorrentsInfoResponse {
        return TorrentsInfoResponse(
            addedOn = Instant.now().toEpochMilli().toInt(),
            amountLeft = 0,
            autoTmm = false,
            availability = 1.0.toFloat(),
            category = "",
            completed = 100,
            completionOn = Instant.now().plusSeconds(10).toEpochMilli().toInt(),
            contentPath = "",
            dlLimit = 0,
            dlSpeed = 100,
            downloaded = 100,
            downloadedSession = 100,
            eta = 100,
            firstLastPiecePriority = false,
            forceStart = false,
            hash = "",
            lastActivity = 100,
            magnetUri = "",
            maxRatio = 10.0.toFloat(),
            maxSeedingTime = 100,
            name = "",
            numComplete = 100,
            numIncomplete = 100,
            numLeeches = 10,
            numSeeds = 100,
            priority = 1,
            progress = 100.0.toFloat(),
            ratio = 1.0.toFloat(),
            ratioLimit = 1.0.toFloat(),
            savePath = "",
            seedingTime = 100,
            seedingTimeLimit = 100,
            seenComplete = 100,
            seqDl = false,
            size = 100,
            state = State.UPLOADING,
            superSeeding = false,
            tags = "",
            timeActive = 100,
            totalSize = 100,
            tracker = "",
            upLimit = 0,
            uploaded = 100,
            uploadedSession = 100,
            upSpeed = 0
        )
    }

    @GetMapping("/api/v2/torrents/properties")
    fun torrentsProperties(@RequestParam hash: String): TorrentPropertiesResponse {
        return TorrentPropertiesResponse(
            savePath = "",
            creationDate = 1,
            pieceSize = 1,
            comment = "",
            totalWasted = 1,
            totalUploaded = 1,
            totalUploadedSession = 1,
            totalDownloaded = 1,
            totalDownloadedSession = 1,
            upLimit = 0,
            dlLimit = 0,
            timeElapsed = 1,
            seedingTime = 0,
            nbConnections = 1,
            nbConnectionsLimit = 1,
            shareRatio = 1.0.toFloat(),
            additionDate = 1,
            completionDate = 1,
            createdBy = "",
            dlSpeedAvg = 1,
            dlSpeed = 1,
            eta = 1,
            lastSeen = 1,
            peers = 1,
            peersTotal = 1,
            piecesHave = 1,
            piecesNum = 1,
            reannounce = 1,
            seeds = 1,
            seedsTotal = 1,
            totalSize = 1,
            upSpeed = 1,
            upSpeedAvg = 1
        )
    }

    @GetMapping("/api/v2/torrents/files")
    fun torrentFiles(@RequestParam hash: String): TorrentFilesResponse {
        return TorrentFilesResponse(
            0,
            "",
            100,
            100,
            1,
            true,
            pieceRange = listOf(1,100),
            availability = 10.0.toFloat()
        )
    }

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

    data class TorrentPropertiesResponse(
        @JsonAlias("save_path")
        val savePath: String,
        @JsonAlias("creation_date")
        val creationDate: Int,
        @JsonAlias("piece_size")
        val pieceSize: Int,
        val comment: String,
        @JsonAlias("total_wasted")
        val totalWasted: Int,
        @JsonAlias("total_uploaded")
        val totalUploaded: Int,
        @JsonAlias("total_uploaded_session")
        val totalUploadedSession: Int,
        @JsonAlias("total_downloaded")
        val totalDownloaded: Int,
        @JsonAlias("total_downloaded_session")
        val totalDownloadedSession: Int,
        @JsonAlias("up_limit")
        val upLimit: Int,
        @JsonAlias("dl_limit")
        val dlLimit: Int,
        @JsonAlias("time_elapsed")
        val timeElapsed: Int,
        @JsonAlias("seeding_time")
        val seedingTime: Int,
        @JsonAlias("nb_connections")
        val nbConnections: Int,
        @JsonAlias("nb_connections_limit")
        val nbConnectionsLimit: Int,
        @JsonAlias("share_ratio")
        val shareRatio: Float,
        @JsonAlias("addition_date")
        val additionDate: Int,
        @JsonAlias("completion_date")
        val completionDate: Int,
        @JsonAlias("created_by")
        val createdBy: String,
        @JsonAlias("dl_speed_avg")
        val dlSpeedAvg: Int,
        @JsonAlias("dl_speed")
        val dlSpeed: Int,
        val eta: Int,
        @JsonAlias("last_seen")
        val lastSeen: Int,
        val peers: Int,
        @JsonAlias("peers_total")
        val peersTotal: Int,
        @JsonAlias("pieces_have")
        val piecesHave: Int,
        @JsonAlias("pieces_num")
        val piecesNum: Int,
        val reannounce: Int,
        val seeds: Int,
        @JsonAlias("seeds_total")
        val seedsTotal: Int,
        @JsonAlias("total_size")
        val totalSize: Int,
        @JsonAlias("up_speed_avg")
        val upSpeedAvg: Int,
        @JsonAlias("up_speed")
        val upSpeed: Int
    )

    data class TorrentsInfoResponse(
        @JsonAlias("added_on")
        val addedOn: Int,
        @JsonAlias("amount_left")
        val amountLeft: Int,
        @JsonAlias("auto_tmm")
        val autoTmm: Boolean,
        val availability: Float,
        val category: String,
        val completed: Int,
        @JsonAlias("completion_id")
        val completionOn: Int,
        @JsonAlias("content_path")
        val contentPath: String,
        @JsonAlias("dl_limit")
        val dlLimit: Int,
        @JsonAlias("dlspeed")
        val dlSpeed: Int,
        val downloaded: Int,
        @JsonAlias("downloaded_session")
        val downloadedSession: Int,
        val eta: Int,
        @JsonAlias("f_l_piece_prio")
        val firstLastPiecePriority: Boolean,
        @JsonAlias("force_start")
        val forceStart: Boolean,
        val hash: String,
        @JsonAlias("last_activity")
        val lastActivity: Int,
        @JsonAlias("magnet_uri")
        val magnetUri: String,
        @JsonAlias("max_ration")
        val maxRatio: Float,
        @JsonAlias("max_seeding_time")
        val maxSeedingTime: Int,
        val name: String,
        @JsonAlias("num_complete")
        val numComplete: Int,
        @JsonAlias("num_incomplete")
        val numIncomplete: Int,
        @JsonAlias("num_leechs")
        val numLeeches: Int,
        @JsonAlias("num_seeds")
        val numSeeds: Int,
        val priority: Int,
        val progress: Float,
        val ratio: Float,
        @JsonAlias("ratio_limit")
        val ratioLimit: Float,
        @JsonAlias("save_path")
        val savePath: String,
        @JsonAlias("seeding_time")
        val seedingTime: Int,
        @JsonAlias("seeding_time_limit")
        val seedingTimeLimit: Int,
        @JsonAlias("seen_complete")
        val seenComplete: Int,
        @JsonAlias("seq_dl")
        val seqDl: Boolean,
        val size: Int,
        val state: State,
        @JsonAlias("super_seeding")
        val superSeeding: Boolean,
        val tags: String,
        @JsonAlias("time_active")
        val timeActive: Int,
        @JsonAlias("total_size")
        val totalSize: Int,
        val tracker: String,
        @JsonAlias("upl_limit")
        val upLimit: Int,
        val uploaded: Int,
        @JsonAlias("uploaded_session")
        val uploadedSession: Int,
        @JsonAlias("upspeed")
        val upSpeed: Int

    )

    enum class State {
        DOWNLOADING, UPLOADING
    }

}