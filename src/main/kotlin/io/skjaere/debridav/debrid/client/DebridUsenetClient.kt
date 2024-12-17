package io.skjaere.debridav.debrid.client

import io.skjaere.debridav.debrid.client.torbox.model.usenet.CreateUsenetDownloadResponse
import io.skjaere.debridav.debrid.client.torbox.model.usenet.GetUsenetListItem
import io.skjaere.debridav.fs.DebridProvider
import org.springframework.web.multipart.MultipartFile

interface DebridUsenetClient {
    suspend fun addNzb(nzbFile: MultipartFile): CreateUsenetDownloadResponse

    suspend fun getDownloads(ids: List<String>): List<GetUsenetListItem>

    fun getProvider(): DebridProvider
}
