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
import io.skjaere.debridav.sabnzbd.UsenetDownloadStatus
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.Instant
import java.util.*

@Service
@Transactional
class DebridUsenetService(
    private val debridUsenetClients: List<DebridUsenetClient>,
    private val torBoxUsenetClient: TorBoxUsenetClient,
    private val usenetRepository: UsenetRepository,
    private val categoryRepository: CategoryRepository,
    private val fileService: FileService,
    private val debridavConfiguration: DebridavConfiguration
) {
    private val mutex = Mutex()
    private val logger = LoggerFactory.getLogger(DebridUsenetService::class.java)

    init {
        File("${debridavConfiguration.filePath}/nzbs/").toPath().let {
            if (!it.exists()) {
                it.createParentDirectories()
            }
        }
    }

    suspend fun addNzb(nzbFile: MultipartFile, category: String): CreateUsenetDownloadResponse =
        withContext(Dispatchers.IO) {
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

            response
        }

    suspend fun getDownloads(): List<UsenetDownload> =
        withContext(Dispatchers.IO) {
            usenetRepository.findAll()
        }.toList()

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    fun updateDownloads() = runBlocking {
        mutex.withLock {
            val allDownloads = usenetRepository.findAll()

            val inProgressDownloadIds = allDownloads
                .asSequence()
                .filter { it.completed == false }
                .filter { it.status != UsenetDownloadStatus.FAILED }
                .filter { it.debridProvider == DebridProvider.TORBOX }
                .map { it.debridId!! }
                .toMutableList()

            val debridDownloads = torBoxUsenetClient.getDownloads(inProgressDownloadIds)

            val (completedDebridDownloads, inProgressDebridDownloads) = debridDownloads
                .partition { it.downloadFinished }

            val updatedInProgressDebridDownloads = inProgressDebridDownloads
                .map { inProgressDebridDownload ->
                    val usenetDownload =
                        allDownloads.first { it.debridId == inProgressDebridDownload.id }
                    usenetDownload.size = inProgressDebridDownload.size
                    usenetDownload.percentCompleted = inProgressDebridDownload.progress
                    usenetDownload.status =
                        UsenetDownloadStatus.valueOf(inProgressDebridDownload.downloadState.uppercase(Locale.getDefault()))
                    usenetDownload
                }
            usenetRepository.saveAll(updatedInProgressDebridDownloads)


            completedDebridDownloads.forEach { completedDownload ->
                completedDownload
                    .files
                    .map { torBoxUsenetClient.getCachedFilesFromUsenetInfoListItem(it, completedDownload.id) }
                    .forEach { file ->
                        val debridFileContents = DebridUsenetFileContents(
                            originalPath = file.path, //TODO: fix me
                            size = file.size,
                            modified = Instant.now().toEpochMilli(),
                            debridLinks = mutableListOf(file),
                            usenetDownloadId = completedDownload.id,
                            nzbFileLocation = "${debridavConfiguration.filePath}/nzbs/${completedDownload.id}/bin.nzb"

                        )
                        fileService.createDebridFile(
                            "${debridavConfiguration.downloadPath}/${file.path}",
                            debridFileContents
                        )
                    }
                val usenetDownload = allDownloads.first { it.debridId == completedDownload.id }
                usenetDownload.completed = true
                usenetDownload.percentCompleted = 1.0
                usenetDownload.status = UsenetDownloadStatus.COMPLETED
                usenetDownload.size = completedDownload.size
                logger.info("saving download: ${usenetDownload.debridId}")
                usenetRepository.save(usenetDownload)
                inProgressDownloadIds.remove(completedDownload.id)
                logger.info("in progress: $inProgressDownloadIds")
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
