package br.com.storehouse.service

import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.entities.Organizacao
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.SolicitacaoInterna
import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import br.com.storehouse.data.model.SolicitacaoInternaRequest
import br.com.storehouse.data.model.SolicitacaoInternaUpdateRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.SolicitacaoInternaRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.*

class SolicitacaoInternaServiceTest {

    private val repo: SolicitacaoInternaRepository = Mockito.mock(SolicitacaoInternaRepository::class.java).also { r ->
        Mockito.`when`(r.save(Mockito.any(SolicitacaoInterna::class.java)))
            .thenAnswer { it.arguments[0] as SolicitacaoInterna }
    }
    private val filialRepo: FilialRepository = Mockito.mock(FilialRepository::class.java)
    private val produtoRepo: ProdutoRepository = Mockito.mock(ProdutoRepository::class.java)
    private val service = SolicitacaoInternaService(repo, filialRepo, produtoRepo)

    private val filialId = UUID.randomUUID()
    private val outraFilialId = UUID.randomUUID()

    private fun filial(id: UUID = filialId) = Filial(id = id, organizacao = Organizacao())

    private fun produtoComEstoque(qtd: Int): Produto {
        val produto = Mockito.mock(Produto::class.java)
        val estado = ProdutoEstado(
            produto = produto,
            estoque = qtd,
            preco = BigDecimal("10.00"),
            precoCusto = BigDecimal("5.00")
        )
        Mockito.`when`(produto.estadoAtual).thenReturn(estado)
        Mockito.`when`(produto.id).thenReturn(UUID.randomUUID())
        Mockito.`when`(produto.nome).thenReturn("Camiseta")
        return produto
    }

    @Test
    fun `criar usa a filial e o email do usuario autenticado, nao do body`() {
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial()))

        val resp = service.criar(
            filialId, "staff@pib.com",
            SolicitacaoInternaRequest(descricaoItem = "Camiseta P", quantidade = 5)
        )

        assertEquals("staff@pib.com", resp.solicitanteEmail)
        assertEquals(StatusSolicitacaoInterna.SOLICITADO, resp.status)
        assertEquals(5, resp.quantidade)
    }

    @Test
    fun `receber soma o estoque criando um novo ProdutoEstado, sem mutar o atual`() {
        val produto = produtoComEstoque(0)
        val estadoAnterior = produto.estadoAtual!!
        val sol = SolicitacaoInterna(
            filial = filial(), produto = produto, descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.COMPRADO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        service.atualizar(
            filialId, sol.id.toString(),
            SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.RECEBIDO)
        )

        // o estado anterior foi FECHADO, nao mutado
        assertNotNull(estadoAnterior.dataFim)
        assertEquals(0, estadoAnterior.estoque)
        // um estado NOVO foi criado com o estoque somado
        Mockito.verify(produto).estadoAtual = Mockito.argThat<ProdutoEstado> { it.estoque == 7 }
        assertNotNull(sol.dataRecebimento)
    }

    @Test
    fun `receber sem produto vinculado e rejeitado`() {
        val sol = SolicitacaoInterna(
            filial = filial(), produto = null, descricaoItem = "Coisa nova",
            quantidade = 3, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.COMPRADO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.RECEBIDO)
            )
        }
    }

    @Test
    fun `receber duas vezes e rejeitado - nao soma estoque de novo`() {
        val sol = SolicitacaoInterna(
            filial = filial(), produto = produtoComEstoque(7), descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.RECEBIDO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.RECEBIDO)
            )
        }
    }

    @Test
    fun `solicitacao de outra filial e invisivel`() {
        val sol = SolicitacaoInterna(
            filial = filial(outraFilialId), descricaoItem = "X",
            quantidade = 1, solicitanteEmail = "outro@pib.com"
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        assertThrows(EntidadeNaoEncontradaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.CANCELADO)
            )
        }
    }
}
