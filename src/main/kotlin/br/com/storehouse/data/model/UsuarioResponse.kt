package br.com.storehouse.data.model

import java.util.UUID

data class UsuarioResponse(
    val id: UUID,
    val username: String?,
    val email: String,
    val perfil: String
)