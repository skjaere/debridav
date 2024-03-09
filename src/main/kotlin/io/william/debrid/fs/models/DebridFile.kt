package io.william.debrid.fs.models

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne


@Entity
open class DebridFile: DebriDavFile {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO )
    override var id: Long? = null
    override var name: String? = null
    override var path: String? = null
    override var size: Long? = null
    @ManyToOne
    override var directory: Directory? = null
    open var link: String? = null
    open var streamingLink: String? = null

    enum class Type { DEBRID, LOCAL}
}
