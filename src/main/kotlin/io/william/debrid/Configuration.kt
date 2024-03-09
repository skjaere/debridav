package io.william.debrid

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.milton.servlet.MiltonFilter
import io.milton.servlet.SpringMiltonFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.PrintWriter
import java.util.*
import javax.sql.DataSource


@Configuration
class FilterConfiguration {


    @Bean
    fun someFilterRegistration(): FilterRegistrationBean<SpringMiltonFilter> {
        val registration = FilterRegistrationBean<SpringMiltonFilter>()
        registration.setFilter(getMiltonFilter());
        registration.setName("MiltonFilter");
        registration.addUrlPatterns("/*");
        registration.addInitParameter("milton.exclude.paths", "/files,/api,/version,/sabnzbd")
        registration.addInitParameter("resource.factory.class",
            "io.william.debrid.resource.StreamableResourceFactory");
        registration.addInitParameter("controllerPackagesToScan",
            "io.william.debrid")
        registration.addInitParameter("contextConfigClass", "io.william.debrid.MiltonConfiguration")
        //registration.addInitParameter("milton.configurator",1 )
        //registration.order = 1
        return registration;
    }

    private fun getMiltonFilter() : SpringMiltonFilter {
        return  SpringMiltonFilter()
    }
}