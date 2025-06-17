package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

@Entity
@IdClass(OrganizacaoUsuarioId::class)
@Table(name = "organizacao_usuario")
class OrganizacaoUsuario(

    @Id
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    var usuario: Usuario,

    @Id
    @ManyToOne
    @JoinColumn(name = "organizacao_id")
    var organizacao: Organizacao,

    @Id
    @ManyToOne
    @JoinColumn(name = "perfil_id")
    var perfil: Perfil
)

data class OrganizacaoUsuarioId(
    val usuario: UUID = UUID.randomUUID(),
    val organizacao: UUID = UUID.randomUUID(),
    val perfil: UUID = UUID.randomUUID()
) : Serializable