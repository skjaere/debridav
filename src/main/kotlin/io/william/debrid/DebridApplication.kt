package io.william.debrid

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class DebridApplication : SpringBootServletInitializer()

fun main(args: Array<String>) {
    runApplication<DebridApplication>(*args)
}

