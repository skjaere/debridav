package io.william.debrid.fs

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne


@Entity
class File {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO )
    var id: Long? = null
    var path: String? = null
    var size: Long? = null
    @ManyToOne
    var directory: Directory? = null
    var link: String? = null
    var streamingLink: String? = null
}
