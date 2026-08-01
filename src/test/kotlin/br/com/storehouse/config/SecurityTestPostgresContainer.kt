package br.com.storehouse.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Container Postgres dedicado a `CatalogoPublicoControllerSecurityTest`, separado do
 * singleton [PostgresTestContainer].
 *
 * `PostgresTestContainer` é compartilhado por TODA a suíte no profile "test" — e por design
 * o Liquibase só roda uma vez nele (contexto cacheado pela mesma assinatura de
 * configuração entre `ProdutoEstadoServiceIntegrationTest`, `ProdutoEstadoConcorrenciaTest`
 * e `CucumberSpringConfig`). `CatalogoPublicoControllerSecurityTest` precisa de um profile
 * DIFERENTE ("sectest", ver comentário na classe do teste) para que `SecurityConfig` fique
 * ativo — e isso obriga um Spring context novo, com seu próprio `LiquibaseTestRunner`
 * (`isDropFirst = true`). Rodar esse segundo `dropFirst` contra o MESMO container
 * compartilhado falha: `dropAll()` só limpa o schema padrão (`public`), não o schema
 * `pinguim` que o changelog também popula — a segunda migração colide com
 * "relation \"sabor\" already exists". Um container próprio, vazio, evita a colisão.
 */
class KPostgreSQLContainerSecTest(image: String) : PostgreSQLContainer<KPostgreSQLContainerSecTest>(image)

object SecurityTestPostgresContainer {

    private val container = KPostgreSQLContainerSecTest("postgres:15-alpine")
        .withDatabaseName("sectestdb")
        .withUsername("sectest")
        .withPassword("sectest")

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
