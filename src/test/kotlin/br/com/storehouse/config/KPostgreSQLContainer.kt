package br.com.storehouse.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer

class KPostgreSQLContainer(image: String) : PostgreSQLContainer<KPostgreSQLContainer>(image)

object PostgresTestContainer {

    private val container = KPostgreSQLContainer("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")

    init {
        container.start()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(ctx: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=${container.jdbcUrl}",
                "spring.datasource.username=${container.username}",
                "spring.datasource.password=${container.password}",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.liquibase.enabled=false"
            ).applyTo(ctx.environment)
        }
    }
}
