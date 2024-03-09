package io.william.debrid.qbittorrent

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.Instant

@Entity
open class Torrent {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO )
    open var id: Long? = null

    open var name: String? = null

    @ManyToOne
    open var category: Category? = null

    @OneToMany
    open var files: List<TorrentFile>? = null

    open var created: Instant? = null

    open var hash: String? = null

    open var savePath: String? = null
}

@Entity
class TorrentFile {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO )
    var id: Long? = null

    var fileName: String? = null

    var size: Long? = null

    var path: String? = null
}
