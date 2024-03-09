package io.william.debrid.fs.models

import jakarta.persistence.ManyToOne

sealed interface DebriDavFile {
    var id: Long?

    var name: String?

    var path: String?

    var size: Long?

    var directory: Directory?
}