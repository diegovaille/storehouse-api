package br.com.storehouse.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VendaResponseContractTest {
    @Test
    fun `VendaResponse mantem os campos publicos esperados`() {
        val nomes = VendaResponse::class.java.declaredFields.map { it.name }.toSet()
        assertEquals(
            setOf("id", "valorTotal", "data", "vendedorNome", "vendedorEmail", "cancelada", "itens"),
            nomes
        )
    }

    @Test
    fun `ItemVendaResponse mantem os campos publicos esperados`() {
        val nomes = ItemVendaResponse::class.java.declaredFields.map { it.name }.toSet()
        assertEquals(
            setOf("produtoNome", "categoria", "quantidade", "precoUnitario", "estoque", "precoCusto"),
            nomes
        )
    }
}
