package io.skjaere.debridav.sabnzbd

import org.springframework.web.multipart.MultipartFile

data class SabnzbdApiRequest(
    /*val name: String,
    val nzbname: String,
    val password: String,
    val cat: String,
    val priority: String,
    val pp: String,*/
    val mode: String,
    val cat: String?,
    val name: MultipartFile?
)

enum class SabnzbdMode { VERSION, GET_CONFIG }