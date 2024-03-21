package io.william.debridav.repository

import io.william.debridav.qbittorrent.Category
import io.william.debridav.qbittorrent.Torrent
import org.springframework.data.repository.CrudRepository

interface TorrentRepository : CrudRepository<Torrent, Long> {
    fun findByCategory(category: Category): List<Torrent>
    fun getByHash(hash: String): Torrent?
}