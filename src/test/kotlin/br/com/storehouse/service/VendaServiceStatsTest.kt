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
}
