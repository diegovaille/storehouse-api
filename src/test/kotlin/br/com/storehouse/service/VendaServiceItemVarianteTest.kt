package br.com.storehouse.service

import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.entities.Organizacao
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.TipoProduto
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.entities.Venda
import br.com.storehouse.data.entities.VendaItem
import br.com.storehouse.data.model.ItemVendaRequest
import br.com.storehouse.data.model.PagamentoVendaRequest
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoEstadoRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.data.repository.VendaRepository
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class VendaServiceItemVarianteTest {

    private val vendaRepo: VendaRepository = Mockito.mock(VendaRepository::class.java).also { repo ->
        Mockito.`when`(repo.save(Mockito.any(Venda::class.java))).thenAnswer { it.arguments[0] as Venda }
    }
    private val produtoRepo: ProdutoRepository = Mockito.mock(ProdutoRepository::class.java)
    private val usuarioRepo: UsuarioRepository = Mockito.mock(UsuarioRepository::class.java)
    private val filialRepo: FilialRepository = Mockito.mock(FilialRepository::class.java)
    private val produtoEstadoRepo: ProdutoEstadoRepository = Mockito.mock(ProdutoEstadoRepository::class.java)
    private val produtoEstadoService = ProdutoEstadoService(produtoRepo, produtoEstadoRepo)

    private val service = VendaService(vendaRepo, produtoRepo, usuarioRepo, filialRepo, produtoEstadoService)

    @Test
    fun `registrarVenda grava cor e tamanho do item`() {
        val filialId = UUID.randomUUID()
        val usuario = Usuario(id = UUID.randomUUID(), username = "diego", email = "a@a.com")
        Mockito.`when`(usuarioRepo.findByEmail("a@a.com")).thenReturn(usuario)
        val filial = Mockito.mock(br.com.storehouse.data.entities.Filial::class.java)
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial))

        val produto = Mockito.mock(Produto::class.java)
        Mockito.`when`(produto.nome).thenReturn("Camiseta Logo")
        val tipo = Mockito.mock(TipoProduto::class.java)
        Mockito.`when`(tipo.nome).thenReturn("Camiseta")
        Mockito.`when`(produto.tipo).thenReturn(tipo)
        val estado = ProdutoEstado(
            produto = produto, preco = BigDecimal("50.00"), estoque = 10,
            dataInicio = LocalDateTime.now(), precoCusto = BigDecimal("10.00")
        )
        Mockito.`when`(produto.estadoAtual).thenReturn(estado)
        val produtoId = UUID.randomUUID()
        Mockito.`when`(produto.id).thenReturn(produtoId)
        Mockito.`when`(produtoRepo.findByIdForUpdate(produtoId)).thenReturn(produto)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse("789", filialId)).thenReturn(produto)
        // ProdutoEstadoService le o estado aberto via query direta (findByProdutoIdAndDataFimIsNull),
        // nao mais via produto.estadoAtual — ver comentario de estadoAbertoDe em ProdutoEstadoService.
        Mockito.`when`(produtoEstadoRepo.findByProdutoIdAndDataFimIsNull(produtoId)).thenReturn(estado)

        val request = VendaRequest(
            voucher = false,
            itens = listOf(ItemVendaRequest(codigoBarras = "789", quantidade = 1, cor = "Azul", tamanho = "G")),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("50.00")))
        )

        val response = service.registrarVenda(filialId, request, "a@a.com")

        assertEquals("Azul", response.itens[0].cor)
        assertEquals("G", response.itens[0].tamanho)
    }

    // As quatro provas abaixo usam um Produto REAL (não mock). Um mock com `estadoAtual`
    // stubado via `Mockito.when(...).thenReturn(...)` mantém o getter retornando o estado
    // ORIGINAL para sempre, mesmo depois que ProdutoEstadoService faz `produto.estadoAtual =
    // novo` internamente — então um teste que precisa observar o efeito CUMULATIVO de duas
    // aplicações de delta sobre o MESMO produto (ex.: dois itens do mesmo produto na mesma
    // venda) não provaria nada com um mock: o segundo `aplicarDelta` sempre leria o estoque
    // original stubado, nunca o já debitado pelo primeiro. Com um Produto real, o setter
    // funciona de verdade e o segundo `aplicarDelta` enxerga o estoque já reduzido.
    private fun produtoReal(
        estoque: Int,
        preco: String = "50.00",
        precoCusto: String = "10.00",
        codigoBarras: String = "789"
    ): Produto {
        val tipo = TipoProduto(nome = "Camiseta")
        val filial = Filial(organizacao = Organizacao())
        val produto = Produto(codigoBarras = codigoBarras, nome = "Camiseta Teste", tipo = tipo, filial = filial)
        produto.estadoAtual = ProdutoEstado(
            produto = produto,
            estoque = estoque,
            preco = BigDecimal(preco),
            precoCusto = BigDecimal(precoCusto),
            dataInicio = LocalDateTime.now()
        )
        // ProdutoEstadoService le o estado aberto via query direta (findByProdutoIdAndDataFimIsNull),
        // nao mais via produto.estadoAtual. Usa thenAnswer (nao thenReturn) para ler o valor
        // ATUAL do campo a cada chamada — é isso que faz o segundo aplicarDelta, na prova de
        // "dois itens do mesmo produto", enxergar o estoque já reduzido pelo primeiro.
        Mockito.`when`(produtoEstadoRepo.findByProdutoIdAndDataFimIsNull(produto.id))
            .thenAnswer { produto.estadoAtual }
        return produto
    }

    private fun prepararUsuarioEFilial(filialId: UUID): Usuario {
        val usuario = Usuario(id = UUID.randomUUID(), username = "diego", email = "a@a.com")
        Mockito.`when`(usuarioRepo.findByEmail("a@a.com")).thenReturn(usuario)
        val filial = Mockito.mock(Filial::class.java)
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial))
        return usuario
    }

    @Test
    fun `registrarVenda com estoque insuficiente lanca EstadoInvalidoException e nao persiste a venda`() {
        val filialId = UUID.randomUUID()
        prepararUsuarioEFilial(filialId)

        val produto = produtoReal(estoque = 1)
        Mockito.`when`(produtoRepo.findByIdForUpdate(produto.id)).thenReturn(produto)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse("789", filialId)).thenReturn(produto)

        val request = VendaRequest(
            voucher = false,
            itens = listOf(ItemVendaRequest(codigoBarras = "789", quantidade = 2)),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("100.00")))
        )

        assertThrows(EstadoInvalidoException::class.java) {
            service.registrarVenda(filialId, request, "a@a.com")
        }

        Mockito.verify(vendaRepo, Mockito.never()).save(Mockito.any(Venda::class.java))
    }

    @Test
    fun `precoUnitario do item de venda reflete o preco do produto antes da venda`() {
        val filialId = UUID.randomUUID()
        prepararUsuarioEFilial(filialId)

        val produto = produtoReal(estoque = 10, preco = "37.50")
        val precoAntesDaVenda = produto.estadoAtual!!.preco
        Mockito.`when`(produtoRepo.findByIdForUpdate(produto.id)).thenReturn(produto)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse("789", filialId)).thenReturn(produto)

        val request = VendaRequest(
            voucher = false,
            itens = listOf(ItemVendaRequest(codigoBarras = "789", quantidade = 1)),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("37.50")))
        )

        val response = service.registrarVenda(filialId, request, "a@a.com")

        assertEquals(precoAntesDaVenda, response.itens[0].precoUnitario)
    }

    @Test
    fun `dois itens do mesmo produto na mesma venda sao avaliados cumulativamente contra o estoque`() {
        val filialId = UUID.randomUUID()
        prepararUsuarioEFilial(filialId)

        // Estoque 3; dois itens de 2 unidades cada somam 4 > 3. Isoladamente cada item (2 <= 3)
        // passaria, então só uma checagem cumulativa (que enxergue o efeito do primeiro item
        // antes de avaliar o segundo) rejeita esta venda.
        val produto = produtoReal(estoque = 3)
        Mockito.`when`(produtoRepo.findByIdForUpdate(produto.id)).thenReturn(produto)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse("789", filialId)).thenReturn(produto)

        val request = VendaRequest(
            voucher = false,
            itens = listOf(
                ItemVendaRequest(codigoBarras = "789", quantidade = 2),
                ItemVendaRequest(codigoBarras = "789", quantidade = 2)
            ),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("200.00")))
        )

        assertThrows(EstadoInvalidoException::class.java) {
            service.registrarVenda(filialId, request, "a@a.com")
        }
    }

    @Test
    fun `cancelar uma venda ja cancelada nao restaura o estoque duas vezes`() {
        val filialId = UUID.randomUUID()
        val produto = produtoReal(estoque = 5)
        Mockito.`when`(produtoRepo.findByIdForUpdate(produto.id)).thenReturn(produto)

        val filial = Mockito.mock(Filial::class.java)
        Mockito.`when`(filial.id).thenReturn(filialId)
        val usuario = Mockito.mock(Usuario::class.java)

        val venda = Venda(
            vendedor = usuario,
            filial = filial,
            valorTotal = BigDecimal("100.00"),
            cancelada = true
        )
        venda.itens = listOf(
            VendaItem(venda = venda, produto = produto, quantidade = 2, precoUnitario = BigDecimal("50.00"))
        )
        Mockito.`when`(vendaRepo.findById(venda.id)).thenReturn(Optional.of(venda))

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.cancelarVenda(filialId, venda.id.toString())
        }

        assertEquals(5, produto.estadoAtual!!.estoque)
    }

    @Test
    fun `registrarVenda com quantidade zero ou negativa lanca RequisicaoInvalidaException`() {
        val filialId = UUID.randomUUID()
        prepararUsuarioEFilial(filialId)

        val produto = produtoReal(estoque = 10)
        Mockito.`when`(produtoRepo.findByIdForUpdate(produto.id)).thenReturn(produto)
        Mockito.`when`(produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse("789", filialId)).thenReturn(produto)

        val request = VendaRequest(
            voucher = false,
            itens = listOf(ItemVendaRequest(codigoBarras = "789", quantidade = -1)),
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("50.00")))
        )

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.registrarVenda(filialId, request, "a@a.com")
        }

        Mockito.verify(vendaRepo, Mockito.never()).save(Mockito.any(Venda::class.java))
        // estoque nao deve ter sido alterado (nem "somado" por uma quantidade negativa)
        assertEquals(10, produto.estadoAtual!!.estoque)
    }
}
