package io.william.debrid

import com.fasterxml.jackson.databind.ObjectMapper
import io.william.debrid.premiumize.PremiumizeClient
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ResourceLoader
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.xml.bind.annotation.adapters.HexBinaryAdapter


@Controller
class SabNzbdEmulationController(
    private val resourceLoader: ResourceLoader,
    private val objectMapper: ObjectMapper,
    private val premiumizeClient: PremiumizeClient
) {
    @GetMapping("sabnzbd/api", produces = ["application/json"])
    fun test(
        @RequestParam mode: String,
        @RequestParam apikey: String,
        @RequestParam output: String,
    ): ResponseEntity<String> {
        val resp = when(mode) {
            "get_config" ->
                resourceLoader
                    .getResource("classpath:sabnzbd_config_response.json")
                    .getContentAsString(Charsets.UTF_8)
            "version" -> objectMapper.writeValueAsString(mapOf("version" to "4.2.2"))
            else -> "hello"
        }
        return ResponseEntity.ok(resp)
    }
    @PostMapping("sabnzbd/api", produces = ["application/json"])
    fun post(
        @RequestParam mode: String,
        @RequestParam apikey: String,
        @RequestParam output: String,
        @RequestPart name: MultipartFile,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<String> {
        val hash = name.inputStream.use { input->
            val sha1 = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var len: Int = input.read(buffer)

            while (len != -1) {
                sha1.update(buffer, 0, len)
                len = input.read(buffer)
            }

            HexBinaryAdapter().marshal(sha1.digest())
        }
        val isCached = premiumizeClient.isCached(hash)
        return ResponseEntity.ok(hash)
    }

}