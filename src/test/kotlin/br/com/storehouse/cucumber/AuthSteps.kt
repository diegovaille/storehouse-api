package br.com.storehouse.cucumber

import br.com.pinguimice.admin.controller.PinguimLoginRequest
import br.com.pinguimice.admin.controller.PinguimLoginResponse
import br.com.storehouse.data.entities.*
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class AuthSteps : BaseSteps() {
    @Autowired private lateinit var pinguimAuthController: br.com.pinguimice.admin.controller.PinguimAuthController

    companion object {
        private const val PINGUIM_ICE_CNPJ = "60774613000108"
    }

    private val passwordEncoder = BCryptPasswordEncoder()
    private var lastAuthResponse: ResponseEntity<Any>? = null
    private var usuarioAtual: Usuario? = null

    // ==================== GIVEN ====================

    @Given("que existe um usuário {string} com senha {string}")
    fun criarUsuario(email: String, senha: String) {
        val usuario = Usuario(
            username = email,
            email = email,
            password = passwordEncoder.encode(senha)
        )
        usuarioRepository.save(usuario)

        this.usuarioAtual = usuario
    }

    @Given("que esse usuário pertence ao Pinguim Ice na filial {string}")
    fun vincularUsuarioAoPinguimIce(filialNome: String) {
        val organizacao = criarOuBuscarOrganizacaoPinguimIce()
        val filial = criarFilial(filialNome, organizacao)
        val perfil = criarOuBuscarPerfilAdmin()

        vincularUsuarioAOrganizacao(usuarioAtual!!, organizacao, perfil)
    }

    @Given("que esse usuário pertence à organização {string} com CNPJ {string}")
    fun vincularUsuarioAOrganizacao(orgNome: String, cnpj: String) {
        val organizacao = Organizacao(
            nome = orgNome,
            cnpj = cnpj,
            logoUrl = "http://example.com/logo.png"
        )
        organizacaoRepository.save(organizacao)

        val filial = criarFilial("Filial Principal", organizacao)
        val perfil = criarOuBuscarPerfilAdmin()

        vincularUsuarioAOrganizacao(usuarioAtual!!, organizacao, perfil)
    }

    // ==================== WHEN ====================

    @When("eu tento fazer login no Pinguim Ice com usuário {string} e senha {string}")
    fun tentarLogin(username: String, senha: String) {
        val request = PinguimLoginRequest(username, senha)
        lastAuthResponse = try {
            pinguimAuthController.login(request)
        } catch (e: Exception) {
            lastException = e
            null
        }
    }

    // ==================== THEN ====================

    @Then("o sistema deve retornar um token de acesso válido")
    fun validarTokenRetornado() {
        assertNotNull(lastAuthResponse, "Response não deveria ser nulo")
        assertEquals(HttpStatus.OK, lastAuthResponse!!.statusCode)

        val body = lastAuthResponse!!.body
        assertTrue(body is PinguimLoginResponse, "Body deveria ser PinguimLoginResponse")

        val response = body as PinguimLoginResponse
        assertNotNull(response.token, "Token não deveria ser nulo")
        assertTrue(response.token.isNotBlank(), "Token não deveria estar vazio")
    }

    @Then("o sistema deve retornar o email {string}")
    fun validarEmailRetornado(emailEsperado: String) {
        val response = lastAuthResponse!!.body as PinguimLoginResponse
        assertEquals(emailEsperado, response.email)
    }

    @Then("o sistema deve retornar o perfil {string}")
    fun validarPerfilRetornado(perfilEsperado: String) {
        val response = lastAuthResponse!!.body as PinguimLoginResponse
        assertEquals(perfilEsperado, response.perfil)
    }

    @Then("o sistema deve retornar erro {string}")
    fun validarErroRetornado(mensagemEsperada: String) {
        assertNotNull(lastAuthResponse, "Response não deveria ser nulo")
        assertNotEquals(HttpStatus.OK, lastAuthResponse!!.statusCode)

        val body = lastAuthResponse!!.body as? Map<*, *>
        assertNotNull(body, "Body deveria ser um Map com erro")
        assertEquals(mensagemEsperada, body!!["error"])
    }

    @Then("o sistema deve retornar status {int}")
    fun validarStatusCode(statusCode: Int) {
        assertNotNull(lastAuthResponse)
        assertEquals(statusCode, lastAuthResponse!!.statusCode.value())
    }

    @Then("o sistema deve negar acesso para usuários fora do Pinguim Ice")
    fun validarAcessoNegado() {
        assertNotNull(lastAuthResponse)
        assertEquals(HttpStatus.FORBIDDEN, lastAuthResponse!!.statusCode)

        val body = lastAuthResponse!!.body as Map<*, *>
        assertTrue(
            body["error"].toString().contains("Pinguim Ice"),
            "Mensagem de erro deveria mencionar restrição do Pinguim Ice"
        )
    }

    // ==================== HELPERS ====================

    private fun criarOuBuscarOrganizacaoPinguimIce(): Organizacao {
        return organizacaoRepository.findByCnpj(PINGUIM_ICE_CNPJ)
            ?: Organizacao(
                nome = "Pinguim Ice",
                cnpj = PINGUIM_ICE_CNPJ,
                logoUrl = "http://pinguimice.com.br/logo.png"
            ).also { organizacaoRepository.save(it) }
    }

    private fun criarFilial(nome: String, organizacao: Organizacao): Filial {
        val filial = Filial(nome = nome, organizacao = organizacao)
        filialRepository.save(filial)

        // Atualiza cache L1 do Hibernate
        (organizacao.filiais as MutableList).add(filial)

        return filial
    }

    private fun criarOuBuscarPerfilAdmin(): Perfil {
        return perfilRepository.findByTipo("ADMIN")
            ?: Perfil(tipo = "ADMIN").also { perfilRepository.save(it) }
    }

    private fun vincularUsuarioAOrganizacao(
        usuario: Usuario,
        organizacao: Organizacao,
        perfil: Perfil
    ) {
        val vinculo = OrganizacaoUsuario(
            usuario = usuario,
            organizacao = organizacao,
            perfil = perfil
        )
        organizacaoUsuarioRepository.save(vinculo)
    }
}