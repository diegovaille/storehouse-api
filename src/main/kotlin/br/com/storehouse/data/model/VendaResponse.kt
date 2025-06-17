package br.com.storehouse.data.model

import java.util.UUID

data class VendaResponse(
    val id: UUID,
    val valorTotal: Double,
    val data: String,
    val vendedorNome: String,
    val vendedorEmail: String,
    val itens: List<ItemVendaResponse>
)

data class ItemVendaResponse(
    val produtoNome: String,
    val quantidade: Int,
    val precoUnitario: Double
)
