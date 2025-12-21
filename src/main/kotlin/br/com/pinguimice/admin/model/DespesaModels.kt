package br.com.pinguimice.admin.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class DespesaRequest(
    val descricao: String,
    val valor: BigDecimal,
    val dataVencimento: String? = null,
    val dataPagamento: String? = null,
    val anexoUrl: String? = null,
    val observacao: String? = null
)

data class DespesaResponse(
    val id: UUID,
    val descricao: String,
    val valor: BigDecimal,
    val dataVencimento: String?,
    val dataPagamento: String?,
    val anexoUrl: String?,
    val observacao: String?,
    val dataCriacao: LocalDateTime
)
