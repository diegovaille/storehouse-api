package br.com.storehouse.service

import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.entities.Venda
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.entities.VendaPagamento
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.data.repository.VendaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

// Mockito.any() returns null which breaks Kotlin non-nullable params; register the matcher then cast.
@Suppress("UNCHECKED_CAST")
private fun <T> anyNonNull(): T { Mockito.any<T>(); return null as T }

class VendaServiceStatsTest {

    private val vendaRepo: VendaRepository = Mockito.mock(VendaRepository::class.java)
    private val produtoRepo: ProdutoRepository = Mockito.mock(ProdutoRepository::class.java)
    private val usuarioRepo: UsuarioRepository = Mockito.mock(UsuarioRepository::class.java)
    private val filialRepo: FilialRepository = Mockito.mock(FilialRepository::class.java)
    private val service = VendaService(vendaRepo, produtoRepo, usuarioRepo, filialRepo)

    private val filialId: UUID = UUID.randomUUID()

    private fun venda(
        total: String,
        voucher: Boolean = false,
        cancelada: Boolean = false,
        data: LocalDateTime = LocalDateTime.now()
    ): Venda = Venda(
        vendedor = Usuario(id = UUID.randomUUID(), username = "v", email = "v@v.com"),
        filial = Mockito.mock(Filial::class.java),
        valorTotal = BigDecimal(total),
        voucher = voucher,
        cancelada = cancelada,
        data = data
    )

    private fun vendaComPagamentos(
        total: String,
        tipos: List<br.com.storehouse.data.enums.TipoPagamento>,
        data: LocalDateTime = LocalDateTime.now()
    ): Venda {
        val v = venda(total, data = data)
        v.pagamentos = tipos.map {
            VendaPagamento(venda = v, tipo = it, valor = BigDecimal(total))
        }
        return v
    }

    private fun stubRange(vendas: List<Venda>) {
        Mockito.`when`(
            vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(
                anyNonNull(),
                anyNonNull(),
                anyNonNull()
            )
        ).thenReturn(vendas)
    }

    @Test
    fun `resumo agrega quantidade total ticket e vouchers ignorando canceladas`() {
        stubRange(listOf(
            venda("10.00", voucher = true),
            venda("20.00"),
            venda("99.00", cancelada = true) // ignorada
        ))

        val r = service.resumoVendas(filialId, null, null)

        assertEquals(2, r.quantidade)
        assertEquals(BigDecimal("30.00"), r.totalArrecadado)
        assertEquals(BigDecimal("15.00"), r.ticketMedio)
        assertEquals(1, r.vouchersUsados)
    }

    @Test
    fun `recentes retorna ate o limite com metodos distintos ignorando canceladas`() {
        stubRange(listOf(
            vendaComPagamentos("10.00", listOf(
                br.com.storehouse.data.enums.TipoPagamento.PIX,
                br.com.storehouse.data.enums.TipoPagamento.PIX
            )),
            vendaComPagamentos("20.00", listOf(
                br.com.storehouse.data.enums.TipoPagamento.CREDITO,
                br.com.storehouse.data.enums.TipoPagamento.PIX
            )),
            venda("30.00", cancelada = true)
        ))

        val recentes = service.vendasRecentes(filialId, 4)

        assertEquals(2, recentes.size)
        assertEquals(listOf("PIX"), recentes[0].metodos)
        assertEquals(listOf("CREDITO", "PIX"), recentes[1].metodos)
        assertEquals(BigDecimal("10.00"), recentes[0].valorTotal)
    }

    @Test
    fun `resumo sem vendas retorna zeros e ticket medio zero sem divisao por zero`() {
        stubRange(emptyList())

        val r = service.resumoVendas(filialId, null, null)

        assertEquals(0, r.quantidade)
        assertEquals(BigDecimal.ZERO, r.totalArrecadado)
        assertEquals(BigDecimal.ZERO, r.ticketMedio)
        assertEquals(0, r.vouchersUsados)
    }

    private fun vendaComItens(itens: List<Triple<String, String, Int>>): Venda {
        // Triple = (produtoNome, categoria, quantidade); precoUnitario fixed at 5.00
        val v = venda("0.00")
        v.itens = itens.map { (nome, cat, qtd) ->
            val produto = Mockito.mock(br.com.storehouse.data.entities.Produto::class.java)
            val tipo = Mockito.mock(br.com.storehouse.data.entities.TipoProduto::class.java)
            Mockito.`when`(produto.nome).thenReturn(nome)
            Mockito.`when`(tipo.nome).thenReturn(cat)
            Mockito.`when`(produto.tipo).thenReturn(tipo)
            br.com.storehouse.data.entities.VendaItem(
                venda = v, produto = produto, quantidade = qtd, precoUnitario = BigDecimal("5.00")
            )
        }
        return v
    }

    @Test
    fun `maisVendidos agrega por produto ordena desc e respeita limite e categoria`() {
        stubRange(listOf(
            vendaComItens(listOf(Triple("Cafe", "Bebidas", 3), Triple("Pao", "Salgados", 1))),
            vendaComItens(listOf(Triple("Cafe", "Bebidas", 2)))
        ))

        val todos = service.maisVendidos(filialId, null, null, 5, null)
        assertEquals("Cafe", todos[0].produtoNome)
        assertEquals(5, todos[0].quantidadeVendida)
        assertEquals(BigDecimal("25.00"), todos[0].totalArrecadado)

        val soSalgados = service.maisVendidos(filialId, null, null, 5, "Salgados")
        assertEquals(1, soSalgados.size)
        assertEquals("Pao", soSalgados[0].produtoNome)
    }

    @Test
    fun `serie preenche dias vazios com zero e cobre a janela`() {
        val hoje = LocalDateTime.now()
        stubRange(listOf(
            venda("10.00", data = hoje),
            venda("5.00", data = hoje.minusDays(2))
        ))

        val serie = service.serieVendas(filialId, 7)

        assertEquals(7, serie.size)
        assertEquals(java.time.LocalDate.now(), serie.last().data)        // ordenado asc
        assertEquals(BigDecimal("10.00"), serie.last().total)
        assertEquals(1, serie.last().quantidade)
        val doisDiasAtras = serie.first { it.data == java.time.LocalDate.now().minusDays(2) }
        assertEquals(BigDecimal("5.00"), doisDiasAtras.total)
        val ontem = serie.first { it.data == java.time.LocalDate.now().minusDays(1) }
        assertEquals(0, ontem.quantidade)
        assertEquals(BigDecimal.ZERO, ontem.total)
    }
}
