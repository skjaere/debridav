package io.skjaere.debridav.debrid

import io.skjaere.debridav.configuration.DebridavConfiguration
import io.skjaere.debridav.debrid.client.DebridUsenetClient
import io.skjaere.debridav.debrid.client.torbox.TorBoxUsenetClient
import io.skjaere.debridav.debrid.client.torbox.model.usenet.CreateUsenetDownloadResponse
import io.skjaere.debridav.debrid.client.torbox.model.usenet.GetUsenetListItem
import io.skjaere.debridav.fs.DebridProvider
import io.skjaere.debridav.fs.DebridUsenetFileContents
import io.skjaere.debridav.fs.FileService
import io.skjaere.debridav.qbittorrent.Category
import io.skjaere.debridav.repository.CategoryRepository
import io.skjaere.debridav.repository.UsenetRepository
import io.skjaere.debridav.sabnzbd.UsenetDownload
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.Instant
import java.util.*

@Service
class DebridUsenetService(
    private val debridUsenetClients: List<DebridUsenetClient>,
    private val torBoxUsenetClient: TorBoxUsenetClient,
    private val usenetRepository: UsenetRepository,
    private val categoryRepository: CategoryRepository,
    private val fileService: FileService,
    private val debridavConfiguration: DebridavConfiguration
) {
    private val mutex = Mutex()

    init {
        File("${debridavConfiguration.filePath}/nzbs/").toPath().let {
            if (!it.exists()) {
                it.createParentDirectories()
            }
        }
    }

    suspend fun addNzb(nzbFile: MultipartFile, category: String): Unit = withContext(Dispatchers.IO) {
        val response = debridUsenetClients.first().addNzb(nzbFile) //TODO: deal with case when nzb is cached
        val usenetDownload = fromCreateUsenetDownloadResponse(
            response,
            nzbFile.originalFilename?.substringBeforeLast(".") ?: UUID.randomUUID().toString(),
            category
        )

        usenetRepository.save(usenetDownload)

        val savedNzbFile = File("${debridavConfiguration.filePath}/nzbs/${usenetDownload.id}/bin.nzb")
        savedNzbFile.toPath().let { if (!it.exists()) it.createParentDirectories() }
        savedNzbFile.writeBytes(nzbFile.bytes)
    }

    suspend fun getDownloads(): List<UsenetDownload> =
        withContext(Dispatchers.IO) {
            usenetRepository.findAll()
        }.toList()

    @Scheduled(fixedDelay = 5_000)
    fun updateDownloads() = runBlocking {
        mutex.withLock {
            val allDownloads = usenetRepository.findAll()

            val inProgressDownloadIds = allDownloads
                .filter { it.completed == false }
                .filter { it.debridProvider == DebridProvider.TORBOX }
                .map { it.debridId!! }

            val debridDownloads = torBoxUsenetClient.getDownloads(inProgressDownloadIds)

            val completedDebridDownloads = debridDownloads
                .filter { it.downloadFinished }

            completedDebridDownloads.forEach { completedDownload ->
                completedDownload
                    .files
                    .map { torBoxUsenetClient.getCachedFilesFromUsenetInfoListItem(it, completedDownload.id) }
                    .forEach { file ->
                        val debridFileContents = DebridUsenetFileContents(
                            originalPath = file.path,
                            size = file.size,
                            modified = Instant.now().toEpochMilli(),
                            debridLinks = mutableListOf(file),
                            usenetDownloadId = completedDownload.id,
                            nzbFileLocation = "${debridavConfiguration.filePath}/nzbs/${completedDownload.downloadId}/bin.nzb"

                        )
                        fileService.createDebridFile(
                            "${debridavConfiguration.downloadPath}/${completedDownload.name}/${file.path}",
                            debridFileContents
                        )
                    }
                val usenetDownload = allDownloads.first { it.debridId == completedDownload.id }
                usenetDownload.completed = true
                usenetDownload.percentCompleted = 1.0
                usenetRepository.save(usenetDownload)
            }
        }
    }

    /*private fun handleDownloadCompletion(completedDownloads: List<GetUsenetListItem>) {
        usenetRepository.setDownloadsToCompleted(completedDownloads.map { it.downloadId })
        completedDownloads.forEach {
            fileService.createDebridFile(

            )
        }
    }*/

    private fun List<GetUsenetListItem>.getByDebridId(debridId: String): GetUsenetListItem =
        this.first { it.downloadId == debridId }

    private fun fromCreateUsenetDownloadResponse(
        response: CreateUsenetDownloadResponse,
        name: String,
        categoryName: String
    ): UsenetDownload {
        val category = categoryRepository.findByName(categoryName) ?: run {
            val newCategory = Category()
            newCategory.name = name
            categoryRepository.save(newCategory)
            newCategory
        }
        val usenetDownload = UsenetDownload()
        usenetDownload.debridId = response.data.usenetDownloadId.toInt()
        usenetDownload.name = name
        usenetDownload.created = Date.from(Instant.now())
        usenetDownload.category = category
        usenetDownload.completed = false
        usenetDownload.percentCompleted = 0.0
        usenetDownload.debridProvider = DebridProvider.TORBOX


        return usenetDownload
    }
}
