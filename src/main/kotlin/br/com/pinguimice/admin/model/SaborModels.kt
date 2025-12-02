package br.com.pinguimice.admin.model

import java.time.LocalDateTime
import java.util.*

data class SaborRequest(
    val nome: String,
    val corHex: String? = null
)

data class SaborResponse(
    val id: UUID,
    val nome: String,
    val corHex: String?,
    val ativo: Boolean,
    val dataCriacao: LocalDateTime
)
