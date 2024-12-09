package io.skjaere.debridav

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SpringBootApplication
class DebridApplication : SpringBootServletInitializer()

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<DebridApplication>(*args)
}

val LOOM: CoroutineDispatcher
    get() = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

val refresherExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
