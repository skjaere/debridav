package io.skjaere.debridav.sabnzbd

import io.skjaere.debridav.debrid.DebridUsenetService
import io.skjaere.debridav.repository.UsenetRepository
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.core.io.ResourceLoader
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@RestController
class SabnzbdApiController(
    private val debridUsenetService: DebridUsenetService,
    private val resourceLoader: ResourceLoader,
    private val usenetRepository: UsenetRepository
) {

    @RequestMapping(
        path = ["/api"],
        method = [RequestMethod.GET, RequestMethod.POST],
    )
    fun addNzb(
        request: SabnzbdApiRequest,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<String> = runBlocking {

        val json = when (request.mode) {
            "version" -> version()
            "get_config" -> config()
            "fullstatus" -> fullStatus()
            "addfile" -> addNzbFile(request)
            "queue" ->

            else -> "else"
        }
        ResponseEntity.ok(json)
    }

    private suspend fun addNzbFile(request: SabnzbdApiRequest): String {
        debridUsenetService.addNzb(
            request.name!!,
            request.cat!!
        )
        return """
                {
                    "status": true,
                    "nzo_ids": ["SABnzbd_nzo_kyt1f0"]
                }
            """.trimIndent()
    }

    private suspend fun queue(): String {
        withContext(Dispatchers.IO) {
            val downloads = usenetRepository.getAllByCreatedAfter(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            val debridServiceResponses = debridUsenetService.getDownloads(
                downloads.filter { it.completed == false}
            ).map {
                UsenetDownload(

                )
            }
        }

    }


    private fun fullStatus(): String = resourceLoader
        .getResource("classpath:sabnzbd_fullstatus.json")
        .getContentAsString(Charsets.UTF_8)

    private fun config(): String = resourceLoader
        .getResource("classpath:sabnzbd_get_config_response.json")
        .getContentAsString(Charsets.UTF_8)

    private fun version() = """{
                            "version": "4.4.0"
                        }"""
}
