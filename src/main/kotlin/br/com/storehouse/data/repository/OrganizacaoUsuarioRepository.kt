package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.OrganizacaoUsuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OrganizacaoUsuarioRepository : JpaRepository<OrganizacaoUsuario, UUID> {
    fun findByUsuarioEmailAndOrganizacaoId(email: String, organizacaoId: UUID): OrganizacaoUsuario?
    fun findAllByUsuarioEmail(email: String): List<OrganizacaoUsuario>
}