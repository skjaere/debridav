package io.william.debrid

import io.milton.config.HttpManagerBuilder
import io.milton.http.HttpManager
import io.milton.http.ProtocolHandlers
import io.milton.http.webdav.DefaultWebDavResponseHandler
import io.milton.http.webdav.WebDavResponseHandler
import io.william.debrid.repository.DirectoryRepository
import io.william.debrid.repository.FileRepository
import io.william.debrid.resource.StreamableResourceFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MiltonConfiguration {
    @Bean("milton.http.manager")
    fun ttpManagerBuilder(directoryRepository: DirectoryRepository, fileRepository: FileRepository): HttpManagerBuilder {
        val builder =HttpManagerBuilder()
        builder.resourceFactory = StreamableResourceFactory(directoryRepository, fileRepository)
        return builder
    }
}