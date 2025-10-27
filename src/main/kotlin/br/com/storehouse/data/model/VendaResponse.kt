package br.com.storehouse.data.model

import java.math.BigDecimal
import java.util.UUID

data class VendaResponse(
    val id: UUID,
    val valorTotal: BigDecimal,
    val data: String,
    val vendedorNome: String,
    val vendedorEmail: String,
    val cancelada: Boolean,
    val itens: List<ItemVendaResponse>
)

data class ItemVendaResponse(
    val produtoNome: String,
    val categoria: String,
    val quantidade: Int,
    val precoUnitario: BigDecimal,
    val estoque: Int?,
    val precoCusto: BigDecimal?
)
