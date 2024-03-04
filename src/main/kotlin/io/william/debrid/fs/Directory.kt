package io.william.debrid.fs

import com.fasterxml.jackson.annotation.JsonIgnore
import io.milton.resource.Resource
import io.william.debrid.resource.DirectoryResource
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.Instant
import java.util.*

@Entity
class Directory {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO )
    var id: Long? = null

    var path: String? = null

    @ManyToOne
    var parent: Directory? = null

}