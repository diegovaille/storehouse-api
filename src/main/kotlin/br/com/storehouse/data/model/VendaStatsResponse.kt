package br.com.storehouse.data.model

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ResumoVendasResponse(
    val quantidade: Int,
    val totalArrecadado: BigDecimal,
    val ticketMedio: BigDecimal,
    val vouchersUsados: Int,
    val cancelados: Int
)

data class VendaRecenteResponse(
    val id: UUID,
    val data: String,            // ISO LocalDateTime, same format as VendaResponse.data
    val metodos: List<String>,   // distinct TipoPagamento names
    val valorTotal: BigDecimal
)

data class ProdutoMaisVendidoResponse(
    val produtoNome: String,
    val categoria: String,
    val quantidadeVendida: Int,
    val totalArrecadado: BigDecimal
)

data class VendaDiaResponse(
    val data: LocalDate,         // yyyy-MM-dd
    val quantidade: Int,
    val total: BigDecimal
)
