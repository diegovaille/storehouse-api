package br.com.storehouse.api.controller

import br.com.storehouse.config.LiquibaseTestRunner
import br.com.storehouse.config.SecurityTestPostgresContainer
import br.com.storehouse.config.TestApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Prova, através da pilha REAL de Spring Security (`SecurityConfig` de produção, o mesmo
 * `RateLimitFilter` que roda em produção — não uma cópia nem um MockMvc standalone que
 * ignora a cadeia), o comportamento do rate limiting por IP.
 *
 * Mesmo profile "sectest" de `CatalogoPublicoControllerSecurityTest` (`SecurityConfig` é
 * `@Profile("!test")`, então sob o profile "test" usado pelo resto da suíte ele fica
 * desligado de propósito — "sectest" satisfaz "!test" e ativa a cadeia de produção de
 * verdade). `application-sectest.yml` liga `server.forward-headers-strategy: framework`
 * (igual a `application-{dev,prod}.yml`) para que o `X-Forwarded-For` de cada requisição vire
 * `request.remoteAddr` de verdade via `ForwardedHeaderFilter`, exatamente como em produção
 * atrás do proxy — é isso que prova a armadilha 1 (chave por IP real, não um IP
 * compartilhado).
 *
 * Propositalmente NÃO sobrescreve `rate-limit.*` para uma cota pequena: o contexto Spring
 * deste profile é cacheado e reutilizado entre esta classe e `CatalogoPublicoControllerSecurityTest`
 * (mesma assinatura de configuração), e o `RateLimitFilter` é um bean singleton — uma cota
 * global baixa estouraria também o bucket "IP default" das chamadas daquela classe (que não
 * setam `X-Forwarded-For`), quebrando testes de outra classe que nada sabem sobre rate
 * limiting. Em vez disso, os testes abaixo batem a cota REAL de produção (60 na vitrine, 10
 * no login) a partir de um IP sintético só deles — mais devagar, mas sem nenhum risco de
 * interferência cruzada com o resto da suíte.
 */
@SpringBootTest(
    classes = [TestApplication::class, LiquibaseTestRunner::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("sectest")
@ContextConfiguration(initializers = [SecurityTestPostgresContainer.Initializer::class])
class RateLimitFilterSecurityTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    companion object {
        // Filial não precisa existir: CatalogoPublicoController devolve 200 com lista vazia
        // para filial inexistente (ver CatalogoPublicoControllerSecurityTest) — só a cota
        // importa aqui, não o conteúdo da resposta.
        private val FILIAL_QUALQUER = UUID.randomUUID()
    }

    @Test
    fun `limite da vitrine publica dispara 429 apos exceder a cota, com Retry-After`() {
        val ip = "203.0.113.10"

        // rate-limit.publico.capacity = 60 (default de produção, não sobrescrito no sectest —
        // ver comentário da classe).
        repeat(60) {
            mockMvc.perform(get("/api/publico/catalogo/$FILIAL_QUALQUER").header("X-Forwarded-For", ip))
                .andExpect(status().isOk)
        }

        mockMvc.perform(get("/api/publico/catalogo/$FILIAL_QUALQUER").header("X-Forwarded-For", ip))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
    }

    @Test
    fun `IPs diferentes tem cotas independentes — armadilha 1, chave por IP real`() {
        val ipEsgotado = "203.0.113.20"
        val ipIntacto = "203.0.113.21"

        repeat(60) {
            mockMvc.perform(get("/api/publico/catalogo/$FILIAL_QUALQUER").header("X-Forwarded-For", ipEsgotado))
        }
        mockMvc.perform(get("/api/publico/catalogo/$FILIAL_QUALQUER").header("X-Forwarded-For", ipEsgotado))
            .andExpect(status().isTooManyRequests)

        // O segundo IP nunca bateu nesta rota — se o filtro chaveasse numa constante (ou no
        // IP do proxy), este request cairia no MESMO bucket já esgotado e voltaria 429 também.
        mockMvc.perform(get("/api/publico/catalogo/$FILIAL_QUALQUER").header("X-Forwarded-For", ipIntacto))
            .andExpect(status().isOk)
    }

    @Test
    fun `limite de login dispara 429 apos exceder a cota`() {
        val ip = "203.0.113.30"
        val corpoLoginInvalido = """{"username":"nao-existe-${UUID.randomUUID()}","password":"errada"}"""

        // rate-limit.login.capacity = 5 (default de produção). Credencial errada é esperado
        // (401) — o que se testa aqui é o limitador, não a autenticação em si; um 401 conta
        // como uma requisição contra o bucket do mesmo jeito que um 200 contaria.
        repeat(5) {
            mockMvc.perform(
                post("/api/auth/login")
                    .header("X-Forwarded-For", ip)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoLoginInvalido)
            ).andExpect(status().isUnauthorized)
        }

        mockMvc.perform(
            post("/api/auth/login")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLoginInvalido)
        ).andExpect(status().isTooManyRequests)
    }

    @Test
    fun `rota autenticada nao e afetada pelo rate limiter`() {
        val ip = "203.0.113.40"

        // Mais requisições do que a cota da vitrine pública (3) ou do login (2) — se este
        // filtro estivesse (por engano) aplicando algum limite aqui, apareceria um 429 no
        // meio dessas 10. GET /api/produtos exige autenticação: sem token, é sempre 401, e é
        // exatamente essa resposta constante que prova que o rate limiter nunca entra em jogo.
        repeat(10) {
            mockMvc.perform(get("/api/produtos").header("X-Forwarded-For", ip))
                .andExpect(status().isUnauthorized)
        }
    }
}
