package br.com.pinguimice.admin.model

import java.time.LocalDateTime
import java.util.*

data class RegiaoVendaRequest(
    val nome: String,
    val descricao: String? = null
)

data class RegiaoVendaResponse(
    val id: UUID,
    val nome: String,
    val descricao: String?,
    val ativo: Boolean,
    val dataCriacao: LocalDateTime
)
