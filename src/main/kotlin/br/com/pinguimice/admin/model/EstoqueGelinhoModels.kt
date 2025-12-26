package br.com.pinguimice.admin.model

import java.time.LocalDateTime
import java.util.*

data class EstoqueGelinhoResponse(
    val id: UUID,
    val saborId: UUID,
    val saborNome: String,
    val quantidade: Int,
    val ultimaAtualizacao: LocalDateTime
)
