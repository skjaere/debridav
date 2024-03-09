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

    @PostMapping("/directory/create", produces = ["application/json"])
    fun createDirectory(@RequestBody body: JsonNode): String {
        fileService.getOrCreateDirectory(body.get("path").asText())
        return "ok"
    }

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
        /*try {
            *//*val isRead = (out!! as CoyoteOutputStream).isReady()
            logger.info("isready: $isRead")*//*

            range?.let {
                val start = range.start ?: 0
                val finish = range.finish ?: debridFile.size!!
                val byteRange = "bytes=$start-$finish"
                connection.setRequestProperty("Range", byteRange)
            }
            *//*if(range != null) {
                *//**//*if(range.start != null && range.finish == null) {
                    RangeUtils.writeRange(connection.getInputStream(), Range(0,range.start), out)
                } else {*//**//*
                    //RangeUtils.writeRange(connection.getInputStream(), range!!, out)
               // }
            } else {

            }*//*
            connection.getInputStream().transferTo(out)
            //.openConnection()
            //.getInputStream()

            *//*connection.use

            connection*//*
        } catch (e: Exception) {
            out!!.close()
            logger.error("error!", e)
        }*/
    }
}