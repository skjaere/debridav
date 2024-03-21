package io.william.debridav

import io.milton.config.HttpManagerBuilder
import io.william.debridav.fs.FileService
//import io.william.debrid.repository.FileRepository
import io.william.debridav.resource.StreamableResourceFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MiltonConfiguration {
    @Bean("milton.http.manager")
    fun httpManagerBuilder(fileService: FileService): HttpManagerBuilder {
        val builder = HttpManagerBuilder()
        builder.resourceFactory = StreamableResourceFactory(fileService)
        return builder
    }
}