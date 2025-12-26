package br.com.pinguimice.admin.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class EmbalagemRequest(
    val nome: String,
    val saborId: UUID? = null,
    val quantidadeKg: BigDecimal,
    val precoKg: BigDecimal
)

data class EmbalagemResponse(
    val id: UUID,
    val nome: String,
    val saborId: UUID?,
    val saborNome: String?,
    val quantidadeKg: BigDecimal,
    val precoKg: BigDecimal,
    val totalUnidades: Int,
    val precoPorUnidade: BigDecimal,
    val estoqueUnidades: Int,
    val dataCriacao: LocalDateTime
)
