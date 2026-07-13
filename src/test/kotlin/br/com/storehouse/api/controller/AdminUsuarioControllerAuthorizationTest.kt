package br.com.storehouse.api.controller

import br.com.storehouse.data.model.NovoUsuarioRequest
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.model.UsuarioResponse
import br.com.storehouse.service.AdminUserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID

/**
 * Prova que o @PreAuthorize de AdminUsuarioController.create é de fato aplicado pelo Spring
 * Method Security, e não apenas "parece certo".
 *
 * Sobe um ApplicationContext mínimo com @EnableMethodSecurity — o mesmo mecanismo que
 * SecurityConfig ativa em produção — e injeta autenticações com EXATAMENTE a mesma authority
 * que o JwtAuthenticationFilter produz em runtime:
 *   SimpleGrantedAuthority("ROLE_${perfil.uppercase()}")
 *
 * Sem um Spring proxy em volta do controller, chamar controller.create(...) diretamente nunca
 * dispara a checagem de autorização — por isso o bean é obtido via @Autowired do contexto,
 * não instanciado com "new".
 *
 * Obs: o mock de AdminUserService é stubado com valores concretos (não com Mockito.any()) para
 * evitar o problema clássico Kotlin/Mockito de checagem de nulidade em parâmetros não-nuláveis;
 * isso não tem relação com a checagem de autorização em si.
 */
@SpringJUnitConfig(classes = [AdminUsuarioControllerAuthorizationTest.TestConfig::class])
class AdminUsuarioControllerAuthorizationTest {

    companion object {
        private val ORGANIZACAO_ID: UUID = UUID.randomUUID()
        private val FILIAL_ID: UUID = UUID.randomUUID()

        private val REQUEST = NovoUsuarioRequest(
            username = "novo.usuario",
            password = "senha123",
            email = "novo@pib.com",
            perfil = "VENDEDOR"
        )

        private val RESPONSE = UsuarioResponse(
            id = UUID.randomUUID(),
            username = REQUEST.username,
            email = REQUEST.email,
            perfil = REQUEST.perfil
        )
    }

    @Configuration
    @EnableMethodSecurity
    class TestConfig {
        @Bean
        fun adminUserService(): AdminUserService {
            val service = Mockito.mock(AdminUserService::class.java)
            Mockito.`when`(service.createUserForSameOrganization(REQUEST, ORGANIZACAO_ID))
                .thenReturn(RESPONSE)
            return service
        }

        @Bean
        fun adminUsuarioController(adminUserService: AdminUserService): AdminUsuarioController =
            AdminUsuarioController(adminUserService)
    }

    @Autowired
    private lateinit var controller: AdminUsuarioController

    @AfterEach
    fun limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext()
    }

    private fun autenticarComo(perfil: String): UsuarioAutenticado {
        val principal = UsuarioAutenticado(
            email = "user@pib.com",
            perfil = perfil,
            organizacaoId = ORGANIZACAO_ID,
            filialId = FILIAL_ID
        )
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${perfil.uppercase()}"))
        )
        SecurityContextHolder.getContext().authentication = authentication
        return principal
    }

    @Test
    fun `principal com ROLE_ADMIN passa pela checagem de autorizacao`() {
        val principal = autenticarComo("ADMIN")

        val resposta = controller.create(REQUEST, principal)

        assertEquals("novo@pib.com", resposta.email)
    }

    @Test
    fun `principal com ROLE_VENDEDOR e barrado com AccessDeniedException (403)`() {
        val principal = autenticarComo("VENDEDOR")

        assertThrows(AccessDeniedException::class.java) {
            controller.create(REQUEST, principal)
        }
    }
}
