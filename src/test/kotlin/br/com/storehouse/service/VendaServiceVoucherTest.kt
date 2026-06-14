package br.com.storehouse.service

import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.TipoProduto
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.entities.Venda
import br.com.storehouse.data.model.ItemVendaRequest
import br.com.storehouse.data.model.PagamentoVendaRequest
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.data.repository.VendaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class VendaServiceVoucherTest {

    private val vendaRepo: VendaRepository = Mockito.mock(VendaRepository::class.java).also { repo ->
        Mockito.`when`(repo.save(Mockito.any(Venda::class.java))).thenAnswer { it.arguments[0] as Venda }
    }
    private val produtoRepo: ProdutoRepository = Mockito.mock(ProdutoRepository::class.java)
    private val usuarioRepo: UsuarioRepository = Mockito.mock(UsuarioRepository::class.java)
    private val filialRepo: FilialRepository = Mockito.mock(FilialRepository::class.java)

    private val service = VendaService(
        vendaRepo = vendaRepo,
        produtoRepo = produtoRepo,
        usuarioRepository = usuarioRepo,
        filialRepository = filialRepo
    )

    @Test
    fun `registrarVenda com voucher aplica 50pct apenas uma vez`() {
        val filialId = UUID.randomUUID()
        val usuario = Usuario(id = UUID.randomUUID(), username = "diego", email = "a@a.com")
        Mockito.`when`(usuarioRepo.findByEmail("a@a.com")).thenReturn(usuario)
        val filial = Mockito.mock(br.com.storehouse.data.entities.Filial::class.java)
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial))

        val produto = Mockito.mock(Produto::class.java)
        Mockito.`when`(produto.nome).thenReturn("Livro Teste")
        val tipo = Mockito.mock(TipoProduto::class.java)
        Mockito.`when`(tipo.nome).thenReturn("Livro")
        Mockito.`when`(produto.tipo).thenReturn(tipo)

        // ProdutoEstado referencia 'produto' no construtor; pra evitar NPEs internas,
        // usamos um estado com referências coerentes.
        val estadoAtual = ProdutoEstado(
            produto = produto,
            preco = BigDecimal("25.00"),
            estoque = 10,
            dataInicio = LocalDateTime.now(),
            precoCusto = BigDecimal("1.00")
        )
        Mockito.`when`(produto.estadoAtual).thenReturn(estadoAtual)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse("123", filialId)).thenReturn(produto)

        val request = VendaRequest(
            voucher = true,
            itens = listOf(ItemVendaRequest(codigoBarras = "123", quantidade = 2)),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("25.00")))
        )

        val response = service.registrarVenda(filialId, request, "a@a.com")

        // preco 25 * 2 = 50; voucher 50% => 25
        assertEquals(BigDecimal("25.00"), response.valorTotal)
    }

    private fun mockProduto(nome: String, preco: String, barras: String, filialId: UUID): Produto {
        val produto = Mockito.mock(Produto::class.java)
        Mockito.`when`(produto.nome).thenReturn(nome)
        val tipo = Mockito.mock(TipoProduto::class.java)
        Mockito.`when`(tipo.nome).thenReturn("Livro")
        Mockito.`when`(produto.tipo).thenReturn(tipo)
        val estado = ProdutoEstado(
            produto = produto,
            preco = BigDecimal(preco),
            estoque = 10,
            dataInicio = LocalDateTime.now(),
            precoCusto = BigDecimal("1.00")
        )
        Mockito.`when`(produto.estadoAtual).thenReturn(estado)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse(barras, filialId))
            .thenReturn(produto)
        return produto
    }

    @Test
    fun `v2 - voucher desconta 50pct de uma unidade (item unico)`() {
        val filialId = UUID.randomUUID()
        val usuario = Usuario(id = UUID.randomUUID(), username = "diego", email = "a@a.com")
        Mockito.`when`(usuarioRepo.findByEmail("a@a.com")).thenReturn(usuario)
        val filial = Mockito.mock(br.com.storehouse.data.entities.Filial::class.java)
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial))

        mockProduto(nome = "Livro Teste", preco = "25.00", barras = "123", filialId = filialId)

        val request = VendaRequest(
            voucher = true,
            itens = listOf(ItemVendaRequest(codigoBarras = "123", quantidade = 2)),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("37.50")))
        )

        val response = service.registrarVenda(filialId, request, "a@a.com", voucherSomenteItemMaisCaro = true)

        // subtotal 25*2 = 50; desconto = 50% de UMA unidade (25) = 12.50; total = 37.50
        assertEquals(BigDecimal("37.50"), response.valorTotal)
    }

    @Test
    fun `v2 - voucher desconta 50pct da unidade mais cara entre produtos`() {
        val filialId = UUID.randomUUID()
        val usuario = Usuario(id = UUID.randomUUID(), username = "diego", email = "a@a.com")
        Mockito.`when`(usuarioRepo.findByEmail("a@a.com")).thenReturn(usuario)
        val filial = Mockito.mock(br.com.storehouse.data.entities.Filial::class.java)
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial))

        mockProduto(nome = "Livro Barato", preco = "25.00", barras = "123", filialId = filialId)
        mockProduto(nome = "Livro Caro", preco = "60.00", barras = "456", filialId = filialId)

        val request = VendaRequest(
            voucher = true,
            itens = listOf(
                ItemVendaRequest(codigoBarras = "123", quantidade = 1),
                ItemVendaRequest(codigoBarras = "456", quantidade = 1)
            ),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("55.00")))
        )

        val response = service.registrarVenda(filialId, request, "a@a.com", voucherSomenteItemMaisCaro = true)

        // subtotal 85; desconto = 50% de 60 (mais caro) = 30; total = 55.00
        assertEquals(BigDecimal("55.00"), response.valorTotal)
    }
}
