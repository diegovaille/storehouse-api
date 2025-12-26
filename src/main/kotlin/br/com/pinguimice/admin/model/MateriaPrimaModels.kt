package br.com.pinguimice.admin.model

import br.com.pinguimice.admin.entity.TipoEntrada
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class MateriaPrimaRequest(
    val nome: String,
    val saborId: UUID? = null,
    val tipoEntrada: TipoEntrada,
    val quantidadeEntrada: BigDecimal,
    val precoEntrada: BigDecimal
)

data class MateriaPrimaResponse(
    val id: UUID,
    val nome: String,
    val saborId: UUID?,
    val saborNome: String?,
    val tipoEntrada: TipoEntrada,
    val quantidadeEntrada: BigDecimal,
    val precoEntrada: BigDecimal,
    val totalUnidades: Int,
    val precoPorUnidade: BigDecimal,
    val estoqueUnidades: Int,
    val dataCriacao: LocalDateTime
)
