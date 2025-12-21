package br.com.storehouse.cucumber

import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.model.UsuarioAutenticado
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class CommonSteps : BaseSteps() {

    @Before
    fun setup() {
        producaoRepository.deleteAll()
        estoqueGelinhoRepository.deleteAll()
        materiaPrimaRepository.deleteAll()
        embalagemRepository.deleteAll()
        outrosRepository.deleteAll()
        vendaItemRepository.deleteAll()
        vendaRepository.deleteAll()
        saborRepository.deleteAll()
        regiaoVendaRepository.deleteAll()
        organizacaoUsuarioRepository.deleteAll()
        despesaRepository.deleteAll()
        clienteRepository.deleteAll()
        filialRepository.deleteAll()
        usuarioRepository.deleteAll()
        organizacaoRepository.deleteAll()
        perfilRepository.deleteAll()
        lastException = null
    }

    @Given("que eu sou um usuário autenticado")
    fun queEuSouUmUsuarioAutenticado() {
        val usuario = Usuario(
            username = "Test User",
            email = "test@pinguimice.com.br",
            password = "password"
        )
        usuarioRepository.save(usuario)

        val userDetails = UsuarioAutenticado(
            email = usuario.email,
            perfil = "ADMIN",
            organizacaoId = UUID.fromString("246a8e9a-6bd5-4b55-a8cb-033de66f676a"),
            filialId = UUID.fromString("bd03afb8-8110-4fc7-ab3e-99b7a140312b")
        )
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }
}
