package io.william.debrid

import org.springframework.test.context.ContextConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ContextConfiguration(initializers = [TestContextInitializer::class], classes = [StubbingService::class])
annotation class MockServerTest