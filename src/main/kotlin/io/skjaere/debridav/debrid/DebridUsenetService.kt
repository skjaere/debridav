package io.skjaere.debridav.debrid

import io.skjaere.debridav.debrid.client.DebridUsenetClient
import io.skjaere.debridav.debrid.client.torbox.model.usenet.CreateUsenetDownloadResponse
import io.skjaere.debridav.debrid.client.torbox.model.usenet.GetUsenetListItem
import io.skjaere.debridav.repository.CategoryRepository
import io.skjaere.debridav.repository.UsenetRepository
import io.skjaere.debridav.sabnzbd.UsenetDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.*

@Service
class DebridUsenetService(
    private val debridUsenetClients: List<DebridUsenetClient>,
    private val usenetRepository: UsenetRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun addNzb(nzbFile: MultipartFile, category: String): ResponseEntity<String> = withContext(Dispatchers.IO) {
        val response = debridUsenetClients.first().addNzb(nzbFile)
        val usenetDownload = fromCreateUsenetDownloadResponse(response, nzbFile.name, category)

        usenetRepository.save(usenetDownload)

        //TODO: create debridfiles

        ResponseEntity.ok().build()
    }

    suspend fun getDownloads(downloads: List<UsenetDownload>): List<GetUsenetListItem> = coroutineScope {
        //val downloads = usenetRepository.getUsenetDownloadsByCompleted(false)
        val debridDownloads = debridUsenetClients
            .map { debridClient ->
                async {
                    debridClient.getDownloads(
                        downloads
                            .filter { it.debridProvider == debridClient.getProvider() }
                            .map { it.debridId!! }
                    )
                }
            }.awaitAll()
            .flatten()
        val completedDebridDownloads = debridDownloads
            .filter { it.downloadFinished }
        usenetRepository.setDownloadsToCompleted(completedDebridDownloads.map { it.hash })

        debridDownloads
    }

    private fun fromCreateUsenetDownloadResponse(
        response: CreateUsenetDownloadResponse,
        name: String,
        category: String
    ): UsenetDownload {
        val usenetDownload = UsenetDownload()
        usenetDownload.debridId = response.data.hash
        usenetDownload.name = name
        usenetDownload.created = Date.from(Instant.now())
        usenetDownload.category = categoryRepository.findByName(category)

        return usenetDownload
    }
}
