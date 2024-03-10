package io.william.debrid.resource

import com.fasterxml.jackson.databind.ObjectMapper
import io.milton.common.RangeUtils
import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.DeletableResource
import io.milton.resource.FileResource
import io.milton.resource.GetableResource
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.DebriDavFile
import io.william.debrid.fs.models.DebridFile
import io.william.debrid.fs.models.LocalDebridFile
import io.william.debrid.fs.models.LocalFile
import org.slf4j.LoggerFactory
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection
import java.time.Instant
import java.util.*
import kotlin.time.measureTime

class FileResource(
    val file: File,
    fileService: FileService
) : AbstractResource(fileService), GetableResource, DeletableResource {


    override fun getUniqueId(): String {
        return file.name.toString()
    }

    override fun getName(): String {
        return file.name
    }

    override fun authorise(request: Request?, method: Request.Method?, auth: Auth?): Boolean {
        return true
    }

    override fun getRealm(): String {
        return "realm"
    }

    override fun getModifiedDate(): Date {
        return Date.from(Instant.now())
    }

    override fun checkRedirect(request: Request?): String? {
        return null
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

    private fun File.isDebridFile(): Boolean = this.name.endsWith(".debridfile")

    override fun sendContent(
        out: OutputStream,
        range: Range?,
        params: MutableMap<String, String>?,
        contentType: String?
    ) {
        /*if(file.isDebridFile())
            sendDebridContent(out,range, params, contentType)
        else*/
        sendLocalContent(out,range, params, contentType)
        /*when(file) {
            is DebridFile, is LocalDebridFile -> sendDebridContent(out,range, params, contentType)
            is LocalFile -> sendLocalContent(out,range, params, contentType)
        }*/
    }
    private fun sendLocalContent(out: OutputStream,
                         range: Range?,
                         params: MutableMap<String, String>?,
                         contentType: String?
    ) {
        val stream = file.inputStream()
        if(range != null) {
            RangeUtils.writeRange(stream, range, out)
        } else {
            stream.transferTo(out)
        }
    }
    /*private fun sendDebridContent(
        out: OutputStream,
        range: Range?,
        params: MutableMap<String, String>?,
        contentType: String?
        ) {
        file as DebridFile
        var connection: URLConnection? = null
        val took = measureTime {
            connection = URL(file.link).openConnection()
        }
        logger.info("opened connection to ${file.link} in $took ms")
        try {
            *//*val isRead = (out!! as CoyoteOutputStream).isReady()
            logger.info("isready: $isRead")*//*

            range?.let {
                if(range.start != null && range.finish == null) {
                    logger.info("Invalid range header received: $range")
                    *//*out.close()
                    connection!!.getInputStream().close()
                    return*//*
                }

                val start = range.start ?: 0
                val finish = range.finish ?: file.size
                val byteRange = "bytes=$start-$finish"
                logger.info("applying byterange: $byteRange from $range")
                connection!!.setRequestProperty("Range", byteRange)
            }
            *//*if(range != null) {
                *//**//*if(range.start != null && range.finish == null) {
                    RangeUtils.writeRange(connection.getInputStream(), Range(0,range.start), out)
                } else {*//**//*
                    //RangeUtils.writeRange(connection.getInputStream(), range!!, out)
               // }
            } else {

            }*//*
            logger.info("Begin streaming of ${file.link}")
            connection!!.getInputStream().transferTo(out)
            logger.info("Streaming of ${file.link} complete")
            connection!!.getInputStream().close()
            out.close()

                //.openConnection()
                //.getInputStream()

            *//*connection.use

            connection*//*
        } catch (e: Exception) {
            out.close()
            connection!!.getInputStream().close()
            logger.error("error!", e)
        }
    }*/

    override fun getMaxAgeSeconds(auth: Auth?): Long {
        return 100
    }

    override fun getContentType(accepts: String?): String? {
        return if(file.isDebridFile())
            "video/mp4"
        else {
            file.toURI().toURL().openConnection().contentType

        }
/*
        return when(file) {
            is DebridFile, is LocalDebridFile -> "video/mp4"
            is LocalFile -> file.contentType
        }
*/
    }

    override fun getContentLength(): Long {
        return file.length()
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return Date.from(Instant.now())
    }

    //fun getDebriDavFile(): DebriDavFile = file
}