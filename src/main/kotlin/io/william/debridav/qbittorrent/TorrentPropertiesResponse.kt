package io.william.debridav.qbittorrent

import com.fasterxml.jackson.annotation.JsonAlias

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
) {
    companion object {
        fun ofTorrent(torrent: Torrent): TorrentPropertiesResponse {
            return TorrentPropertiesResponse(
                    savePath = torrent.savePath!!,
                    creationDate = torrent.created!!.toEpochMilli().toInt(),
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
    }
}