package io.william.debrid.fs.models

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.Instant

@Entity
class Directory {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO )
    var id: Long? = null

    var path: String? = null

    @ManyToOne
    var parent: Directory? = null

    var created: Instant? = null

}