package br.com.pinguimice.admin.model

import java.math.BigDecimal
import java.util.UUID

data class PinguimVendaRequest(
    val regiaoId: UUID,
    val cliente: String?,
    val itens: List<PinguimVendaItemRequest>,
    val total: BigDecimal,
    val totalPago: BigDecimal,
    val abaterEstoque: Boolean = true
)

data class PinguimVendaItemRequest(
    val saborId: UUID,
    val quantidade: Int
)

data class PinguimVendaResponse(
    val id: UUID,
    val total: BigDecimal,
    val totalPago: BigDecimal,
    val dataVenda: String,
    val cliente: String?,
    val regiao: String,
    val vendedor: String,
    val abaterEstoque: Boolean,
    val itens: List<PinguimVendaItemResponse>
)

data class PinguimVendaItemResponse(
    val sabor: String,
    val quantidade: Int
)
