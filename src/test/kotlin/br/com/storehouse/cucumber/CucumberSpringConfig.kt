package br.com.storehouse.cucumber

import br.com.storehouse.config.LiquibaseTestRunner
import br.com.storehouse.config.PostgresTestContainer
import br.com.storehouse.config.TestApplication
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@CucumberContextConfiguration
@SpringBootTest(
    classes = [TestApplication::class, LiquibaseTestRunner::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
class CucumberSpringConfig
