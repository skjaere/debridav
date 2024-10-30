package io.william.debridav.debrid.realdebrid.model

import kotlinx.serialization.Serializable

typealias HashResponse = HashMap<String, List<HosterResponse>>
typealias HosterResponse = HashMap<Int, FileNameAndSize>

@Serializable
data class FileNameAndSize(
    val filename: String,
    val filesize: Long,
)