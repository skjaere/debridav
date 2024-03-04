package io.william.debrid

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["io.william.debrid.repository"])
class PersistenceConfiguration {
}