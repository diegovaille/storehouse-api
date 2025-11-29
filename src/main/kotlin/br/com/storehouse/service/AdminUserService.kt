package br.com.storehouse.service

import br.com.storehouse.data.entities.OrganizacaoUsuario
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.model.NovoUsuarioRequest
import br.com.storehouse.data.model.UsuarioResponse
import br.com.storehouse.data.repository.OrganizacaoRepository
import br.com.storehouse.data.repository.OrganizacaoUsuarioRepository
import br.com.storehouse.data.repository.PerfilRepository
import br.com.storehouse.data.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AdminUserService(
    private val usuarioRepository: UsuarioRepository,
    private val perfilRepository: PerfilRepository,
    private val organizacaoUsuarioRepository: OrganizacaoUsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val organizacaoRepository: OrganizacaoRepository
) {

    @Transactional
    fun createUserForSameOrganization(
        request: NovoUsuarioRequest,
        organizacaoId: UUID
    ): UsuarioResponse {
        if (usuarioRepository.existsByUsername(request.username))
            error("Username já cadastrado!")
        if (usuarioRepository.existsByEmail(request.email))
            error("Email já cadastrado!")

        val perfil = perfilRepository.findByTipo(request.perfil.uppercase())
            ?: error("Perfil Inválido!")

        val usuario = Usuario(
            id = UUID.randomUUID(),
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password)
        )

        val organizacao = organizacaoRepository.findById(organizacaoId).get()
        usuarioRepository.save(usuario)

        val vinculo = OrganizacaoUsuario(
            usuario = usuario,
            organizacao = organizacao,
            perfil = perfil
        )
        organizacaoUsuarioRepository.save(vinculo)

        return UsuarioResponse(
            id = usuario.id,
            username = usuario.username,
            email = usuario.email,
            perfil = perfil.tipo
        )
    }
}
