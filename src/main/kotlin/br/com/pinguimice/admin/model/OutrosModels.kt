package br.com.pinguimice.admin.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class OutrosRequest(
    val nome: String,
    val quantidadeEntrada: Int,
    val precoEntrada: BigDecimal,
    val unidadesPorItem: Int
)

data class OutrosResponse(
    val id: UUID,
    val nome: String,
    val quantidadeEntrada: Int,
    val precoEntrada: BigDecimal,
    val unidadesPorItem: Int,
    val totalUnidades: Int,
    val precoPorUnidade: BigDecimal,
    val estoqueUnidades: Int,
    val dataCriacao: LocalDateTime
)
