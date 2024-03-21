package io.william.debridav.repository

import io.william.debridav.qbittorrent.TorrentFile
import org.springframework.data.repository.CrudRepository

interface TorrentFileRepository : CrudRepository<TorrentFile, Long> {
}