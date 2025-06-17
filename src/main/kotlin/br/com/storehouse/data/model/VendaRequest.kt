package br.com.storehouse.data.model

data class VendaRequest(
    val itens: List<ItemVendaRequest>,
    val pagamentos: List<PagamentoVendaRequest>
)

data class ItemVendaRequest(
    val codigoBarras: String,
    val quantidade: Int
)

data class PagamentoVendaRequest(
    val tipo: String,
    val valor: Double
)