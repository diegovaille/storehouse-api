package br.com.storehouse.data.model

import java.math.BigDecimal

data class VendaRequest(
    val itens: List<ItemVendaRequest>,
    val pagamentos: List<PagamentoVendaRequest>,
    val voucher: Boolean = false
)

data class ItemVendaRequest(
    val codigoBarras: String,
    val quantidade: Int,
    val cor: String? = null,
    val tamanho: String? = null
)

data class PagamentoVendaRequest(
    val tipo: String,
    val valor: BigDecimal
)