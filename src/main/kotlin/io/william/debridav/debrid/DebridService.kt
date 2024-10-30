package io.william.debridav.debrid

import io.william.debridav.LinkCheckService
import io.william.debridav.configuration.DebridavConfiguration
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridProvider
import io.william.debridav.fs.FileService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class DebridService(
    private val debridClients: List<DebridClient>,
    private val debridavConfiguration: DebridavConfiguration,
    private val fileService: FileService,
    private val linkCheckService: LinkCheckService,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(DebridService::class.java)

    fun isCached(magnet: String): Boolean = runBlocking {
        supervisorScope {
            flow {
                debridavConfiguration.debridClients.map { debridProvider ->
                    emit(debridClients.getClient(debridProvider).isCached(magnet))
                }
            }.firstOrNull { it } ?: false
        }
    }

    suspend fun getCheckedLinks(file: File): Flow<CachedFile> {
        val debridFileContents = fileService.getDebridFileContents(file)
        return getFlowOfDebridLinks(debridFileContents)
            .transformWhile { debridLink ->
                updateContentsOfDebridFile(debridFileContents, debridLink, file)
                if (debridLink is CachedFile) {
                    emit(debridLink)
                }
                debridLink is MissingFile
            }
    }

    suspend fun getDebridFiles(magnet: String): List<DebridFileContents> = supervisorScope {
        val torrentFiles = mutableListOf<DebridFileContents>()
        getCachedFilesFlow(magnet).collect { cachedFile ->
            torrentFiles.firstOrNull { sizeAndNameMatches(cachedFile, it) }
                ?.debridLinks?.add(cachedFile)
                ?: run {
                    torrentFiles.add(createDebridFileContents(cachedFile, magnet))
                }
        }
        getFileContentsWithMissingLinks(torrentFiles)
    }

    private fun sizeAndNameMatches(first: CachedFile, debridFileContents: DebridFileContents): Boolean {
        return (first.path.split("/").last() == debridFileContents.originalPath.split("/").last()
                && first.size == debridFileContents.size)
    }

    private fun getFileContentsWithMissingLinks(torrentFiles: MutableList<DebridFileContents>): List<DebridFileContents> {
        return torrentFiles.map { debridFileContents ->
            if (debridFileContents.debridLinks.size < debridClients.size) {
                debridClients.forEach { debridClient ->
                    debridFileContents.debridLinks = getDebridLinksWithMissingFiles(debridFileContents, debridClient)
                }
            }
            debridFileContents
        }
    }

    private fun getDebridLinksWithMissingFiles(
        debridFileContents: DebridFileContents,
        debridClient: DebridClient
    ): MutableList<DebridFile> {
        if (!debridFileContents.debridLinks.any { it.provider == debridClient.getProvider() }) {
            return debridFileContents.debridLinks
                .union(
                    listOf(MissingFile(debridClient.getProvider(), Instant.now(clock).toEpochMilli()))
                ).toMutableList()
        }
        return debridFileContents.debridLinks
    }

    private fun createDebridFileContents(
        cachedFile: CachedFile,
        magnet: String
    ) = DebridFileContents(
        originalPath = cachedFile.path,
        size = cachedFile.size,
        modified = Instant.now(clock).toEpochMilli(),
        magnet = magnet,
        debridLinks = mutableListOf(cachedFile)
    )

    private suspend fun getCachedFilesFlow(magnet: String) =
        getCachedFilesFlow(magnet, debridClients.map { debridClients.getClient(it.getProvider()) })

    private suspend fun getCachedFilesFlow(magnet: String, debridClients: List<DebridClient>) = supervisorScope {
        debridClients
            .map { debridClient ->
                async { debridClient.getCachedFiles(magnet) }
            }.map { it.awaitOrEmptyList() }
            .flatten()
            .asFlow()
    }


    private suspend fun Deferred<List<CachedFile>>.awaitOrEmptyList(): List<CachedFile> {
        return try {
            this.await()
        } catch (e: Exception) {
            logger.error("Error fetching cached files", e)
            listOf()
        }
    }

    private fun updateContentsOfDebridFile(
        debridFileContents: DebridFileContents,
        debridLink: DebridFile,
        file: File
    ) {
        debridFileContents.replaceOrAddDebridLink(debridLink)
        fileService.writeContentsToFile(file, debridFileContents)
    }

    private suspend fun getFlowOfDebridLinks(debridFileContents: DebridFileContents): Flow<DebridFile> = flow {
        debridavConfiguration.debridClients.map { debridProvider ->
            debridFileContents.debridLinks
                .firstOrNull { it.provider == debridProvider }
                ?.let { debridFile -> emitDebridFile(debridFile, debridFileContents, debridProvider) }
                ?: run { emit(getFreshDebridLink(debridFileContents, debridClients.getClient(debridProvider))) }
        }
    }

    private suspend fun FlowCollector<DebridFile>.emitDebridFile(
        debridFile: DebridFile,
        debridFileContents: DebridFileContents,
        debridProvider: DebridProvider
    ) {
        when (debridFile) {
            is CachedFile -> emitWorkingLink(debridFile, debridFileContents, debridProvider)
            is MissingFile -> emitRefreshedResult(debridFile, debridFileContents, debridProvider)
        }
    }

    private suspend fun FlowCollector<DebridFile>.emitRefreshedResult(
        debridFile: DebridFile,
        debridFileContents: DebridFileContents,
        debridProvider: DebridProvider
    ) {
        if (linkShouldBeReChecked(debridFile)) {
            emit(getFreshDebridLink(debridFileContents, debridClients.getClient(debridProvider)))
        } else {
            emit(debridFile)
        }
    }

    private suspend fun FlowCollector<DebridFile>.emitWorkingLink(
        debridFile: CachedFile,
        debridFileContents: DebridFileContents,
        debridProvider: DebridProvider
    ) {
        if (linkCheckService.isLinkAlive(debridFile)) {
            emit(debridFile)
        } else {
            emit(getFreshDebridLink(debridFileContents, debridClients.getClient(debridProvider)))
        }
    }

    private suspend fun getFreshDebridLink(
        debridFileContents: DebridFileContents,
        debridClient: DebridClient
    ): DebridFile {
        return if (debridClient.isCached(debridFileContents.magnet)) {
            getCachedFilesFlow(debridFileContents.magnet, listOf(debridClient))
                .retry(5)
                .filter { fileMatches(it, debridFileContents) }
                .firstOrNull() ?: MissingFile(debridClient.getProvider(), Instant.now(clock).toEpochMilli())
        } else {
            MissingFile(debridClient.getProvider(), Instant.now(clock).toEpochMilli())
        }
    }

    private fun fileMatches(
        it: CachedFile,
        debridFileContents: DebridFileContents
    ) = it.path.split("/").last() == debridFileContents.originalPath.split("/").last()

    private fun linkShouldBeReChecked(debridFile: DebridFile): Boolean {
        return Instant.ofEpochMilli(debridFile.lastChecked).isBefore(
            Instant.now(clock).minus(24, ChronoUnit.HOURS)
        )
    }
}

fun List<DebridClient>.getClient(debridProvider: DebridProvider): DebridClient =
    this.first { it.getProvider() == debridProvider }