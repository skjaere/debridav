package io.william.debrid

import org.apache.commons.io.FileUtils
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.util.TestSocketUtils
import java.io.File

class TestContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val port = TestSocketUtils.findAvailableTcpPort()
        val mockServer: ClientAndServer = startClientAndServer(port)
        applicationContext.addApplicationListener(ApplicationListener<ContextClosedEvent>() {
            mockServer.stop()
            FileUtils.deleteDirectory(File("/tmp/debridavtests"))
        })
        TestPropertyValues.of(
            "premiumize.baseurl=http://localhost:$port",
            "mockserver.port=$port"
        ).applyTo(applicationContext)
    }
}