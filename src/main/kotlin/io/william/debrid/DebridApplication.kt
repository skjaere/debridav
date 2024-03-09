package io.william.debrid

import com.fasterxml.jackson.annotation.JsonInclude
import io.william.debrid.fs.FileService
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@SpringBootApplication
class DebridApplication

fun main(args: Array<String>) {
    runApplication<DebridApplication>(*args)
}

