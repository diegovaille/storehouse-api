package br.com.pinguimice.admin.model

import java.time.LocalDateTime
import java.util.UUID

data class ClienteRequest(
    val nome: String,
    val endereco: String? = null,
    val telefone: String? = null,
    val cnpj: String? = null,
    val regiaoId: UUID? = null,
    val bloqueado: Boolean = false,
    val motivoBloqueio: String? = null
)

data class ClienteResponse(
    val id: UUID,
    val nome: String,
    val endereco: String?,
    val telefone: String?,
    val cnpj: String?,
    val regiao: ClienteRegiaoInfo?,
    val bloqueado: Boolean,
    val motivoBloqueio: String?,
    val dataCriacao: LocalDateTime
)

data class ClienteRegiaoInfo(
    val id: UUID,
    val nome: String
)

