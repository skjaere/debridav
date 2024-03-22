package io.william.debridav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.milton.servlet.SpringMiltonFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


@Configuration
class FilterConfiguration {


    @Bean
    fun someFilterRegistration(): FilterRegistrationBean<SpringMiltonFilter> {
        val registration = FilterRegistrationBean<SpringMiltonFilter>()
        registration.setFilter(getMiltonFilter())
        registration.setName("MiltonFilter")
        registration.addUrlPatterns("/*")
        registration.addInitParameter("milton.exclude.paths", "/files,/api,/version,/sabnzbd")
        registration.addInitParameter(
                "resource.factory.class",
                "io.william.debrid.resource.StreamableResourceFactory"
        )
        registration.addInitParameter(
                "controllerPackagesToScan",
                "io.william.debrid"
        )
        registration.addInitParameter("contextConfigClass", "io.william.debridav.MiltonConfiguration")

        return registration
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()

    private fun getMiltonFilter(): SpringMiltonFilter {
        return SpringMiltonFilter()
    }
}