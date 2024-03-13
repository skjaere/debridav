package io.william.debrid.qbittorrent

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class TorrentsInfoResponse(
    @JsonAlias("added_on")
    @JsonProperty("added_on")
    val addedOn: Int,
    @JsonAlias("amount_left")
    @JsonProperty("amount_left")
    val amountLeft: Int,
    @JsonAlias("auto_tmm")
    @JsonProperty("auto_tmm")
    val autoTmm: Boolean,
    val availability: Float,
    val category: String,
    val completed: Int,
    @JsonAlias("completion_id")
    @JsonProperty("completion_id")
    val completionOn: Int,
    @JsonAlias("content_path")
    @JsonProperty("content_path")
    val contentPath: String,
    @JsonAlias("dl_limit")
    @JsonProperty("dl_limit")
    val dlLimit: Int,
    @JsonAlias("dlspeed")
    @JsonProperty("dlspeed")
    val dlSpeed: Int,
    val downloaded: Int,
    @JsonAlias("downloaded_session")
    @JsonProperty("downloaded_session")
    val downloadedSession: Int,
    val eta: Int,
    @JsonAlias("f_l_piece_prio")
    @JsonProperty("f_l_piece_prio")
    val firstLastPiecePriority: Boolean,
    @JsonAlias("force_start")
    @JsonProperty("force_start")
    val forceStart: Boolean,
    val hash: String,
    @JsonAlias("last_activity")
    @JsonProperty("last_activity")
    val lastActivity: Int,
    @JsonAlias("magnet_uri")
    @JsonProperty("magnet_uri")
    val magnetUri: String,
    @JsonAlias("max_ration")
    @JsonProperty("max_ration")
    val maxRatio: Float,
    @JsonAlias("max_seeding_time")
    @JsonProperty("max_seeding_time")
    val maxSeedingTime: Int,
    val name: String,
    @JsonAlias("num_complete")
    @JsonProperty("num_complete")
    val numComplete: Int,
    @JsonAlias("num_incomplete")
    @JsonProperty("num_incomplete")
    val numIncomplete: Int,
    @JsonAlias("num_leechs")
    @JsonProperty("num_leechs")
    val numLeeches: Int,
    @JsonAlias("num_seeds")
    @JsonProperty("num_seeds")
    val numSeeds: Int,
    val priority: Int,
    val progress: Float,
    val ratio: Float,
    @JsonAlias("ratio_limit")
    @JsonProperty("ratio_limit")
    val ratioLimit: Float,
    @JsonAlias("save_path")
    @JsonProperty("save_path")
    val savePath: String,
    @JsonAlias("seeding_time")
    @JsonProperty("seeding_time")
    val seedingTime: Int,
    @JsonAlias("seeding_time_limit")
    @JsonProperty("seeding_time_limit")
    val seedingTimeLimit: Int,
    @JsonAlias("seen_complete")
    @JsonProperty("seen_complete")
    val seenComplete: Int,
    @JsonAlias("seq_dl")
    @JsonProperty("seq_dl")
    val seqDl: Boolean,
    val size: Int,
    val state: String,
    @JsonAlias("super_seeding")
    @JsonProperty("super_seeding")
    val superSeeding: Boolean,
    val tags: String,
    @JsonAlias("time_active")
    @JsonProperty("time_active")
    val timeActive: Int,
    @JsonAlias("total_size")
    @JsonProperty("total_size")
    val totalSize: Int,
    val tracker: String,
    @JsonAlias("upl_limit")
    @JsonProperty("upl_limit")
    val upLimit: Int,
    val uploaded: Int,
    @JsonAlias("uploaded_session")
    @JsonProperty("uploaded_session")
    val uploadedSession: Int,
    @JsonAlias("upspeed")
    @JsonProperty("upspeed")
    val upSpeed: Int
) {
    companion object {
        fun ofTorrent(torrent: Torrent, downloadDir: String): TorrentsInfoResponse {
            return TorrentsInfoResponse(
                addedOn = torrent.created!!.toEpochMilli().toInt(),
                amountLeft = 0,
                autoTmm = false,
                availability = 1.0.toFloat(),
                category = torrent.category!!.name!!,
                completed = 1,
                completionOn = torrent.created!!.toEpochMilli().toInt(),
                contentPath = "$downloadDir/${torrent.name!!.replace(" ", "_")}/",
                dlLimit = 0,
                dlSpeed = 0,
                downloaded = 100,
                downloadedSession = 100,
                eta = 100,
                firstLastPiecePriority = false,
                forceStart = false,
                hash = torrent.hash!!,
                lastActivity = 100,
                magnetUri = "",
                maxRatio = 10.0.toFloat(),
                maxSeedingTime = 100,
                name = torrent.name!!,
                numComplete = 0,
                numIncomplete = 0,
                numLeeches = 0,
                numSeeds = 0,
                priority = 1,
                progress = 1.0.toFloat(),
                ratio = 1.0.toFloat(),
                ratioLimit = 1.0.toFloat(),
                savePath = "/downloads",
                seedingTime = 0,
                seedingTimeLimit = 0,
                seenComplete = 0,
                seqDl = false,
                size = 100,
                state = "pausedUP",
                superSeeding = false,
                tags = "",
                timeActive = 0,
                totalSize = 100,
                tracker = "",
                upLimit = 0,
                uploaded = 0,
                uploadedSession = 0,
                upSpeed = 0
            )
        }
    }
}