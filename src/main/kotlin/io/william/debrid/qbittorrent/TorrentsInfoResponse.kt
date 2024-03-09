package io.william.debrid.qbittorrent

import com.fasterxml.jackson.annotation.JsonAlias

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
) {
    companion object {
        fun ofTorrent(torrent: Torrent): TorrentsInfoResponse {
            return TorrentsInfoResponse(
                addedOn = torrent.created!!.toEpochMilli().toInt(),
                amountLeft = 0,
                autoTmm = false,
                availability = 1.0.toFloat(),
                category = torrent.category!!.name!!,
                completed = 100,
                completionOn = torrent.created!!.toEpochMilli().toInt(),
                contentPath = "/downloads",
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
                progress = 100.0.toFloat(),
                ratio = 1.0.toFloat(),
                ratioLimit = 1.0.toFloat(),
                savePath = "",
                seedingTime = 0,
                seedingTimeLimit = 0,
                seenComplete = 0,
                seqDl = false,
                size = 100,
                state = State.UPLOADING,
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
    enum class State {
        DOWNLOADING, UPLOADING
    }
}