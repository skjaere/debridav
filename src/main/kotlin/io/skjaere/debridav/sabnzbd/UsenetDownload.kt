package io.skjaere.debridav.sabnzbd

import io.skjaere.debridav.fs.DebridProvider
import io.skjaere.debridav.qbittorrent.Category
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.util.*

@Entity
open class UsenetDownload {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    open var id: Long? = null
    open var name: String? = null
    open var debridId: String? = null
    open var created: Date? = null
    open var completed: Boolean? = null
    open var debridProvider: DebridProvider? = null


    @ManyToOne
    open var category: Category? = null
}
