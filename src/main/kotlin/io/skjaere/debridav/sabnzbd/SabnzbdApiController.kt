package io.skjaere.debridav.sabnzbd

import io.skjaere.debridav.debrid.DebridUsenetService
import io.skjaere.debridav.repository.UsenetRepository
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(SabnzbdApiController::class.java)

    @RequestMapping(
        path = ["/api"],
        method = [RequestMethod.GET, RequestMethod.POST],
    )
    fun addNzb(
        request: SabnzbdApiRequest,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<String> = runBlocking {
        logger.info(httpServletRequest.requestURI.toString())
        val json = when (request.mode) {
            "version" -> version()
            "get_config" -> config()
            "fullstatus" -> fullStatus()
            "addfile" -> addNzbFile(request)
            "queue" -> queue()
            "history" -> history()

            else -> {
                logger.error("unknown mode ${request.mode}")
                "else"
            }
        }
        ResponseEntity.ok(json)
    }

    private fun history(): String {
        val slots = usenetRepository
            .findAll()
            .filter { it.completed!! }
            .map { usenetConversionService.convert(it, HistorySlot::class.java)!! }
        return Json.encodeToString(
            SabnzbdHistoryResponse(
                SabnzbdHistory(
                    slots
                )
            )
        )
    }

    private suspend fun addNzbFile(request: SabnzbdApiRequest): String {
        val response = debridUsenetService.addNzb(
            request.name!!, request.cat!!
        )
        return Json.encodeToString(
            AddNzbResponse(
                response.success,
                listOf(response.data.usenetDownloadId)
            )
        )
    }

    private suspend fun queue(): String {
        val queueSlots = getDownloads()
        logger.info("Queue Slots: $queueSlots")
        val queue = Queue(
            status = "Downloading",
            speedLimit = "0",
            speedLimitAbs = "0",
            paused = false,
            noofSlots = queueSlots.size,
            noofSlotsTotal = queueSlots.size,
            limit = 0,
            start = 0,
            timeLeft = "0:10:0",
            speed = "1 M",
            kbPerSec = "100.0",
            size = "0",//${(queueSlots.sumOf { it.mb.toInt() } / 1000)} GB",
            sizeLeft = "0",//"${(queueSlots.sumOf { it.mbLeft.toInt() } / 1000)} GB",
            mb = "0",//queueSlots.sumOf { it.mb.toInt() }.toString(),
            mbLeft = "0",//queueSlots.sumOf { it.mbLeft.toLong() }.toString(),
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
