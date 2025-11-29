package br.com.storehouse.data.model

data class NovoUsuarioRequest(
    val username: String,
    val password: String,
    val email: String,
    val perfil: String // "ADMIN" or "VENDEDOR"
)