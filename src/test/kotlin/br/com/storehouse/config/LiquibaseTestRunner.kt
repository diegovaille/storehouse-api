package br.com.storehouse.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

@TestConfiguration
class LiquibaseTestRunner {

    @Bean
    fun runLiquibase(dataSource: DataSource): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:/db/changelog/db.changelog-test-master.yaml"
        liquibase.isDropFirst = true
        return liquibase
    }
}
