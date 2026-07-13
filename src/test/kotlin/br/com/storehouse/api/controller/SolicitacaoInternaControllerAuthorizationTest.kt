package br.com.storehouse.api.controller

import br.com.storehouse.api.handler.GlobalExceptionHandler
import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import br.com.storehouse.data.model.SolicitacaoInternaRequest
import br.com.storehouse.data.model.SolicitacaoInternaResponse
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.service.SolicitacaoInternaService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Prova, através da pilha real do Spring MVC (MockMvc + o @RestControllerAdvice de produção),
 * que uma negação de @PreAuthorize vira HTTP 403 — não 500.
 *
 * Encontrado rodando a aplicação de verdade contra um Postgres real: um VENDEDOR chamando
 * POST /api/solicitacoes-internas (guardado por @PreAuthorize("hasRole('ADMIN')")) recebia
 * HTTP 500 com corpo "Erro inesperado: Access Denied". O @ExceptionHandler(Exception) genérico
 * do GlobalExceptionHandler engolia o AccessDeniedException do Spring Security antes que
 * qualquer handler mais específico pudesse tratá-lo. Só apareceu agora porque
 * SolicitacaoInternaController é o primeiro controller com @PreAuthorize de método que de fato
 * NEGA: o de AdminUsuarioController usava hasAuthority('ADMIN'), que nunca batia com o
 * ROLE_ADMIN concedido pelo filtro JWT, então nunca chegava a lançar.
 *
 * Ao contrário de AdminUsuarioControllerAuthorizationTest (que chama o controller diretamente
 * e só prova que a exceção é lançada), este teste passa pelo MockMvc de verdade com o
 * GlobalExceptionHandler registrado como controller advice — por isso ele cobre também a
 * tradução da exceção em resposta HTTP, não só a anotação de segurança.
 */
@SpringJUnitConfig(classes = [SolicitacaoInternaControllerAuthorizationTest.TestConfig::class])
class SolicitacaoInternaControllerAuthorizationTest {

    companion object {
        private val FILIAL_ID: UUID = UUID.randomUUID()
        private const val SOLICITANTE_EMAIL = "user@pib.com"

        // Precisa bater EXATAMENTE com o corpo desserializado de REQUEST_JSON, já que o mock é
        // stubado com um valor concreto (não com Mockito.any()) — ver comentário equivalente em
        // AdminUsuarioControllerAuthorizationTest sobre o problema clássico Kotlin/Mockito de
        // checagem de nulidade em parâmetros não-nuláveis com matchers.
        private val REQUEST = SolicitacaoInternaRequest(
            descricaoItem = "Parafuso M4",
            produtoId = null,
            quantidade = 10,
            observacao = null
        )

        private const val REQUEST_JSON = """
            {"descricaoItem":"Parafuso M4","quantidade":10}
        """

        private val RESPONSE = SolicitacaoInternaResponse(
            id = UUID.randomUUID().toString(),
            descricaoItem = "Parafuso M4",
            produtoId = null,
            produtoNome = null,
            quantidade = 10,
            solicitanteEmail = SOLICITANTE_EMAIL,
            observacao = null,
            status = StatusSolicitacaoInterna.SOLICITADO,
            dataCriacao = "2026-07-12T10:00:00",
            dataAtualizacao = null,
            dataRecebimento = null
        )
    }

    @Configuration
    @EnableMethodSecurity
    class TestConfig {
        @Bean
        fun solicitacaoInternaService(): SolicitacaoInternaService {
            val service = Mockito.mock(SolicitacaoInternaService::class.java)
            Mockito.`when`(service.criar(FILIAL_ID, SOLICITANTE_EMAIL, REQUEST)).thenReturn(RESPONSE)
            return service
        }

        @Bean
        fun solicitacaoInternaController(service: SolicitacaoInternaService): SolicitacaoInternaController =
            SolicitacaoInternaController(service)

        @Bean
        fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
    }

    @Autowired
    private lateinit var controller: SolicitacaoInternaController

    @Autowired
    private lateinit var exceptionHandler: GlobalExceptionHandler

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun montarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionHandler)
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()
    }

    @AfterEach
    fun limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext()
    }

    private fun autenticarComo(perfil: String) {
        val principal = UsuarioAutenticado(
            email = SOLICITANTE_EMAIL,
            perfil = perfil,
            organizacaoId = UUID.randomUUID(),
            filialId = FILIAL_ID
        )
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${perfil.uppercase()}"))
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `VENDEDOR recebe 403 com mensagem de acesso negado, nao 500`() {
        autenticarComo("VENDEDOR")

        mockMvc.perform(
            post("/api/solicitacoes-internas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_JSON)
        )
            .andExpect(status().isForbidden)
            .andExpect(content().string("Acesso negado: perfil sem permissão para esta operação"))
    }

    @Test
    fun `ADMIN passa pela checagem de autorizacao e recebe 200`() {
        autenticarComo("ADMIN")

        mockMvc.perform(
            post("/api/solicitacoes-internas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_JSON)
        )
            .andExpect(status().isOk)
    }
}
