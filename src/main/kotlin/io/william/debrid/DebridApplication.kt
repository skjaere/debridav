package io.william.debrid

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DebridApplication

fun main(args: Array<String>) {
    runApplication<DebridApplication>(*args)
}
