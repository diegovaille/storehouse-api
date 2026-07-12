package br.com.storehouse.service

import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.SolicitacaoInterna
import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import br.com.storehouse.data.model.SolicitacaoInternaRequest
import br.com.storehouse.data.model.SolicitacaoInternaResponse
import br.com.storehouse.data.model.SolicitacaoInternaUpdateRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.SolicitacaoInternaRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class SolicitacaoInternaService(
    private val repo: SolicitacaoInternaRepository,
    private val filialRepository: FilialRepository,
    private val produtoRepository: ProdutoRepository
) {
    @Transactional
    fun criar(filialId: UUID, solicitanteEmail: String, req: SolicitacaoInternaRequest): SolicitacaoInternaResponse {
        if (req.quantidade <= 0) throw RequisicaoInvalidaException("Quantidade deve ser maior que zero")

        val filial = filialRepository.findByIdOrNull(filialId)
            ?: throw EntidadeNaoEncontradaException("Filial não encontrada")

        val produto = req.produtoId?.let { id ->
            produtoRepository.findByIdOrNull(UUID.fromString(id))
                ?.takeIf { it.filial.id == filialId }
                ?: throw EntidadeNaoEncontradaException("Produto $id não encontrado na filial")
        }

        val solicitacao = SolicitacaoInterna(
            filial = filial,
            produto = produto,
            descricaoItem = req.descricaoItem,
            quantidade = req.quantidade,
            solicitanteEmail = solicitanteEmail,
            observacao = req.observacao
        )
        return repo.save(solicitacao).toResponse()
    }

    fun listar(filialId: UUID, status: StatusSolicitacaoInterna?): List<SolicitacaoInternaResponse> {
        val lista = if (status != null) {
            repo.findByFilialIdAndStatusInOrderByDataCriacaoDesc(filialId, listOf(status))
        } else {
            // ativas: aguardando compra ou aguardando chegar
            repo.findByFilialIdAndStatusInOrderByDataCriacaoDesc(
                filialId,
                listOf(StatusSolicitacaoInterna.SOLICITADO, StatusSolicitacaoInterna.COMPRADO)
            )
        }
        return lista.map { it.toResponse() }
    }

    @Transactional
    fun atualizar(
        filialId: UUID,
        id: String,
        req: SolicitacaoInternaUpdateRequest
    ): SolicitacaoInternaResponse {
        val solicitacao = repo.findByIdOrNull(UUID.fromString(id))
            ?: throw EntidadeNaoEncontradaException("Solicitação interna $id não encontrada")
        if (solicitacao.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Solicitação interna $id não pertence à filial")
        }

        // vincular produto (pode vir junto com o RECEBIDO ou antes dele)
        req.produtoId?.let { pid ->
            solicitacao.produto = produtoRepository.findByIdOrNull(UUID.fromString(pid))
                ?.takeIf { it.filial.id == filialId }
                ?: throw EntidadeNaoEncontradaException("Produto $pid não encontrado na filial")
        }

        if (req.status == StatusSolicitacaoInterna.RECEBIDO) {
            receber(solicitacao)
        } else if (req.status != null) {
            solicitacao.status = req.status
        }

        solicitacao.dataAtualizacao = LocalDateTime.now()
        return repo.save(solicitacao).toResponse()
    }

    /**
     * Somar estoque é a única regra de negócio de verdade aqui.
     *
     * O estoque vive em ProdutoEstado, uma cadeia temporal: mudar significa FECHAR o
     * estado atual e criar um novo — nunca mutar em lugar. Ver VendaService.registrarVenda.
     */
    private fun receber(solicitacao: SolicitacaoInterna) {
        if (solicitacao.status == StatusSolicitacaoInterna.RECEBIDO) {
            throw RequisicaoInvalidaException("Solicitação já foi recebida — receber de novo somaria o estoque em dobro")
        }
        if (solicitacao.status == StatusSolicitacaoInterna.CANCELADO) {
            throw RequisicaoInvalidaException("Solicitação cancelada não pode ser recebida")
        }

        val produto = solicitacao.produto
            ?: throw RequisicaoInvalidaException("Vincule um produto antes de receber — sem produto não há onde somar o estoque")

        val estadoAtual = produto.estadoAtual
            ?: throw EstadoInvalidoException("Produto ${produto.id} não possui estado atual definido")

        estadoAtual.dataFim = LocalDateTime.now()

        val novoEstado = ProdutoEstado(
            produto = produto,
            estoque = estadoAtual.estoque + solicitacao.quantidade,
            preco = estadoAtual.preco,
            precoCusto = estadoAtual.precoCusto,
            dataInicio = LocalDateTime.now()
        )
        produto.estadoAtual = novoEstado

        solicitacao.status = StatusSolicitacaoInterna.RECEBIDO
        solicitacao.dataRecebimento = LocalDateTime.now()
    }
}

fun SolicitacaoInterna.toResponse() = SolicitacaoInternaResponse(
    id = this.id.toString(),
    descricaoItem = this.descricaoItem,
    produtoId = this.produto?.id?.toString(),
    produtoNome = this.produto?.nome,
    quantidade = this.quantidade,
    solicitanteEmail = this.solicitanteEmail,
    observacao = this.observacao,
    status = this.status,
    dataCriacao = this.dataCriacao.toString(),
    dataAtualizacao = this.dataAtualizacao?.toString(),
    dataRecebimento = this.dataRecebimento?.toString()
)
