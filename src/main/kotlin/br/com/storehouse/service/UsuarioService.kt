package br.com.storehouse.service

import br.com.storehouse.data.entities.OrganizacaoUsuario
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.repository.OrganizacaoUsuarioRepository
import br.com.storehouse.data.repository.UsuarioRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UsuarioService(
    private val repo: UsuarioRepository,
    private val organizacaoUsuarioRepository: OrganizacaoUsuarioRepository
) : UserDetailsService {

    override fun loadUserByUsername(usernameOrEmail: String): UserDetails {
        val usuario = repo.findByEmail(usernameOrEmail) ?: repo.findByUsername(usernameOrEmail)
        ?: throw UsernameNotFoundException("Usuário não encontrado")

        return User(
            usuario.email,
            usuario.password ?: "",
            listOf(SimpleGrantedAuthority("ROLE_USER")) // default sem vínculo com organização
        )
    }

    fun buscarPorUsername(username: String): Usuario? = repo.findByUsername(username)

    fun buscarPorEmail(email: String): Usuario? = repo.findByEmail(email)

    fun cadastrar(email: String, role: String): Usuario {
        val usuario = Usuario(UUID.randomUUID(), email, role)
        return repo.save(usuario)
    }

    fun buscarOrganizacoesDoUsuario(email: String): List<OrganizacaoUsuario> {
        return organizacaoUsuarioRepository.findAllByUsuarioEmail(email)
    }

    fun buscarPerfilDoUsuarioNaOrganizacao(email: String, organizacaoId: UUID): OrganizacaoUsuario? {
        return organizacaoUsuarioRepository.findByUsuarioEmailAndOrganizacaoId(email, organizacaoId)
    }

    fun senhaValida(usuario: Usuario, rawPassword: String): Boolean {
        // Assumindo que BCrypt é usado:
        return usuario.password != null && BCryptPasswordEncoder().matches(rawPassword, usuario.password)
    }
}
