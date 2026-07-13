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
import br.com.storehouse.data.repository.ProdutoEstadoRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.SolicitacaoInternaRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
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
    private val produtoEstadoRepo: ProdutoEstadoRepository = Mockito.mock(ProdutoEstadoRepository::class.java)
    private val produtoEstadoService = ProdutoEstadoService(produtoRepo, produtoEstadoRepo)
    private val service = SolicitacaoInternaService(repo, filialRepo, produtoRepo, produtoEstadoService)

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
        val produtoId = UUID.randomUUID()
        Mockito.`when`(produto.estadoAtual).thenReturn(estado)
        Mockito.`when`(produto.id).thenReturn(produtoId)
        Mockito.`when`(produto.nome).thenReturn("Camiseta")
        // aplicarDelta trava a linha do produto via findByIdForUpdate antes de ler/mudar o estado
        Mockito.`when`(produtoRepo.findByIdForUpdate(produtoId)).thenReturn(produto)
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
        // um estado NOVO foi criado com o estoque somado, preservando preco e precoCusto
        val captor = ArgumentCaptor.forClass(ProdutoEstado::class.java)
        Mockito.verify(produto).estadoAtual = captor.capture()
        val novoEstado = captor.value
        assertEquals(7, novoEstado.estoque)
        assertEquals(BigDecimal("10.00"), novoEstado.preco)
        assertEquals(BigDecimal("5.00"), novoEstado.precoCusto)
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
    fun `receber solicitacao cancelada e rejeitado`() {
        val sol = SolicitacaoInterna(
            filial = filial(), produto = produtoComEstoque(7), descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.CANCELADO
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

    // --- Máquina de estados: bloqueio de regressão a partir de estado terminal ---

    @Test
    fun `ataque de dupla soma via regressao RECEBIDO para COMPRADO e rejeitado, estoque soma uma unica vez`() {
        val produto = produtoComEstoque(0)
        val sol = SolicitacaoInterna(
            filial = filial(), produto = produto, descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.COMPRADO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        // 1. COMPRADO -> RECEBIDO: soma estoque 0 -> 7
        service.atualizar(
            filialId, sol.id.toString(),
            SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.RECEBIDO)
        )
        assertEquals(StatusSolicitacaoInterna.RECEBIDO, sol.status)

        // 2. RECEBIDO -> COMPRADO: regressao a partir de estado terminal, deve ser rejeitada
        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.COMPRADO)
            )
        }
        assertEquals(StatusSolicitacaoInterna.RECEBIDO, sol.status)

        // 3. Se o passo 2 tivesse passado, este terceiro PATCH somaria o estoque de novo.
        //    Precisa continuar rejeitado (RECEBIDO -> RECEBIDO tambem e regressao terminal).
        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.RECEBIDO)
            )
        }

        // em nenhum momento o estoque foi somado mais de uma vez
        val captor = ArgumentCaptor.forClass(ProdutoEstado::class.java)
        Mockito.verify(produto, Mockito.times(1)).estadoAtual = captor.capture()
        assertEquals(7, captor.value.estoque)
    }

    @Test
    fun `RECEBIDO para CANCELADO e rejeitado`() {
        val sol = SolicitacaoInterna(
            filial = filial(), produto = produtoComEstoque(7), descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.RECEBIDO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.CANCELADO)
            )
        }
        assertEquals(StatusSolicitacaoInterna.RECEBIDO, sol.status)
    }

    @Test
    fun `trocar produtoId de solicitacao ja RECEBIDA e rejeitado`() {
        val produtoOriginal = produtoComEstoque(7)
        val sol = SolicitacaoInterna(
            filial = filial(), produto = produtoOriginal, descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.RECEBIDO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        val outroProdutoId = UUID.randomUUID()

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(produtoId = outroProdutoId.toString())
            )
        }
        // produto original nao foi trocado, e o repositorio de produto nem chegou a ser consultado
        assertEquals(produtoOriginal, sol.produto)
        Mockito.verify(produtoRepo, Mockito.never()).findById(outroProdutoId)
    }

    @Test
    fun `transicao para tras de COMPRADO para SOLICITADO e rejeitada`() {
        val sol = SolicitacaoInterna(
            filial = filial(), descricaoItem = "Camiseta P",
            quantidade = 7, solicitanteEmail = "staff@pib.com",
            status = StatusSolicitacaoInterna.COMPRADO
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.SOLICITADO)
            )
        }
        assertEquals(StatusSolicitacaoInterna.COMPRADO, sol.status)
    }

    // --- Isolamento entre filiais no vinculo de produto ---

    @Test
    fun `criar com produtoId de outra filial e rejeitado`() {
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial()))

        val produtoOutraFilial = Mockito.mock(Produto::class.java)
        Mockito.`when`(produtoOutraFilial.filial).thenReturn(filial(outraFilialId))
        val produtoId = UUID.randomUUID()
        Mockito.`when`(produtoRepo.findById(produtoId)).thenReturn(Optional.of(produtoOutraFilial))

        assertThrows(EntidadeNaoEncontradaException::class.java) {
            service.criar(
                filialId, "staff@pib.com",
                SolicitacaoInternaRequest(
                    descricaoItem = "Camiseta P",
                    produtoId = produtoId.toString(),
                    quantidade = 5
                )
            )
        }
    }

    @Test
    fun `atualizar vinculando produtoId de outra filial e rejeitado`() {
        val sol = SolicitacaoInterna(
            filial = filial(), descricaoItem = "Camiseta P",
            quantidade = 5, solicitanteEmail = "staff@pib.com"
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        val produtoOutraFilial = Mockito.mock(Produto::class.java)
        Mockito.`when`(produtoOutraFilial.filial).thenReturn(filial(outraFilialId))
        val produtoId = UUID.randomUUID()
        Mockito.`when`(produtoRepo.findById(produtoId)).thenReturn(Optional.of(produtoOutraFilial))

        assertThrows(EntidadeNaoEncontradaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(produtoId = produtoId.toString())
            )
        }
    }

    // --- UUID malformado e requisicao invalida (4xx), nao erro de servidor ---

    @Test
    fun `id malformado no path e rejeitado como requisicao invalida`() {
        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, "nao-e-um-uuid",
                SolicitacaoInternaUpdateRequest(status = StatusSolicitacaoInterna.CANCELADO)
            )
        }
    }

    @Test
    fun `produtoId malformado no body e rejeitado como requisicao invalida`() {
        val sol = SolicitacaoInterna(
            filial = filial(), descricaoItem = "Camiseta P",
            quantidade = 5, solicitanteEmail = "staff@pib.com"
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))

        assertThrows(RequisicaoInvalidaException::class.java) {
            service.atualizar(
                filialId, sol.id.toString(),
                SolicitacaoInternaUpdateRequest(produtoId = "nao-e-um-uuid")
            )
        }
    }
}
