package io.skjaere.debridav.debrid

import io.skjaere.debridav.debrid.client.DebridUsenetClient
import io.skjaere.debridav.debrid.model.CachedFile
import io.skjaere.debridav.debrid.model.DebridFile
import io.skjaere.debridav.debrid.model.MissingFile
import io.skjaere.debridav.fs.DebridProvider
import io.skjaere.debridav.fs.DebridUsenetFileContents
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DebridLinkUsenetService {
    private val logger = LoggerFactory.getLogger(DebridLinkUsenetService::class.java)

    suspend fun getFreshDebridLinkFromUsenet(
        debridFileContents: DebridUsenetFileContents,
        debridClient: DebridUsenetClient
    ): DebridFile {
        val debridLink = debridFileContents.debridLinks.first()
        return when (debridLink) {
            is CachedFile -> {
                val cachedFile = debridFileContents.debridLinks.first() as CachedFile
                cachedFile.params["downloadFileId"]!!.let { downloadFileId ->
                    debridClient.getStreamableLink(debridFileContents.usenetDownloadId, downloadFileId)
                        ?.let { freshLink ->
                            return cachedFile.withNewLink(freshLink)
                        } ?: run {
                        logger.warn("No streamable link found for $downloadFileId")
                        MissingFile(DebridProvider.TORBOX, Instant.now().toEpochMilli())
                    }
                }
            }

            else -> MissingFile(DebridProvider.TORBOX, Instant.now().toEpochMilli()) //TODO: handle me
        }

    }
}