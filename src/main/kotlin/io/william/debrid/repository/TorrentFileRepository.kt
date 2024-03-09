package io.william.debrid.repository

import io.william.debrid.qbittorrent.TorrentFile
import org.springframework.data.repository.CrudRepository

interface TorrentFileRepository: CrudRepository<TorrentFile, Long> {
}