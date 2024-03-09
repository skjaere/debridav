package io.william.debrid.repository

import io.william.debrid.qbittorrent.Category
import io.william.debrid.qbittorrent.Torrent
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TorrentRepository : CrudRepository<Torrent, Long> {
    fun findByCategory(category: Category): List<Torrent>
    fun getByHash(hash: String): Torrent?
}