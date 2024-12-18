package io.skjaere.debridav.sabnzbd

import io.skjaere.debridav.debrid.DebridUsenetService
import io.skjaere.debridav.repository.UsenetRepository
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.core.convert.ConversionService
import org.springframework.core.io.ResourceLoader
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class SabnzbdApiController(
    private val debridUsenetService: DebridUsenetService,
    private val resourceLoader: ResourceLoader,
    private val usenetRepository: UsenetRepository,
    private val usenetConversionService: ConversionService
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
            "queue" -> queue()

            else -> "else"
        }
        ResponseEntity.ok(json)
    }

    private suspend fun addNzbFile(request: SabnzbdApiRequest): String {
        debridUsenetService.addNzb(
            request.name!!, request.cat!!
        )
        return """
                {
                    "status": true,
                    "nzo_ids": ["SABnzbd_nzo_kyt1f0"]
                }
            """.trimIndent()
    }

    private suspend fun queue(): String {
        val queueSlots = getDownloads()
        val queue = Queue(
            status = "Downloading",
            speedLimit = "0",
            speedLimitAbs = "0",
            paused = false,
            noofSlots = queueSlots.size,
            noofSlotsTotal = queueSlots.size,
            limit = 0,
            start = 0,
            timeLeft = "1h",
            speed = "1 M",
            kbPerSec = "100.0",
            size = "${(queueSlots.sumOf { it.mb.toInt() } / 1000)} GB",
            sizeLeft = "${(queueSlots.sumOf { it.mbLeft.toInt() } / 1000)} GB",
            mb = queueSlots.sumOf { it.mb.toInt() }.toString(),
            mbLeft = queueSlots.sumOf { it.mbLeft.toLong() }.toString(),
            slots = queueSlots
        )
        return Json.encodeToString(SabnzbdFullListResponse(queue))
    }

    private suspend fun getDownloads(): List<ListResponseDownloadSlot> = withContext(Dispatchers.IO) {
        usenetRepository.findAll()
    }.map { usenetConversionService.convert(it, ListResponseDownloadSlot::class.java)!! }


    private fun fullStatus(): String =
        resourceLoader.getResource("classpath:sabnzbd_fullstatus.json").getContentAsString(Charsets.UTF_8)

    private fun config(): String =
        resourceLoader.getResource("classpath:sabnzbd_get_config_response.json").getContentAsString(Charsets.UTF_8)

    private fun version() = """{
                            "version": "4.4.0"
                        }"""
}
