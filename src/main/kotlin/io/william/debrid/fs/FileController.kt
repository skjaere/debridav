package io.william.debrid.fs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.net.URL

@RestController
@RequestMapping("files")
class FileController(
    private val fileService: FileService
) {
    @PostMapping("/file/create", produces = ["application/json"])
    fun createFile(@RequestBody body: JsonNode): String {
        val mapper = jacksonObjectMapper()
        val req = mapper.convertValue(body, FileService.CreateFileRequest::class.java)
        fileService.createFile(req)
        return "ok"
    }
    @RequestMapping(
        path=  ["stream"],
        method = [RequestMethod.GET],
        produces = ["video/mp4"]
        )
    fun getStream(request: HttpServletRequest,
                  response: HttpServletResponse,
                  @RequestHeader range: String
    ) {
        val inputStream = URL("https://belligerentclam-sto.energycdn.com/dl/vikUu87pSE2hMJkvsWGiug/1710276410/650846778/65b3ef67b385a9.95662541/The.Sopranos.S03E04.Employee.of.the.Month.GBR.BluRay.Remux.1080p.AVC.DTS-HD.MA.5.1-SHeNTo.mkv")
            .openStream()

        inputStream.transferTo(response.outputStream)
        inputStream.close()
        response.outputStream.close()
    }
}