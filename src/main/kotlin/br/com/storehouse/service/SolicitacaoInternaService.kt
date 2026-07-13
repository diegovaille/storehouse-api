package br.com.storehouse.service

import br.com.storehouse.data.entities.SolicitacaoInterna
import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import br.com.storehouse.data.model.SolicitacaoInternaRequest
import br.com.storehouse.data.model.SolicitacaoInternaResponse
import br.com.storehouse.data.model.SolicitacaoInternaUpdateRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.SolicitacaoInternaRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
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
    private val produtoRepository: ProdutoRepository,
    private val produtoEstadoService: ProdutoEstadoService
) {
    @Transactional
    fun criar(filialId: UUID, solicitanteEmail: String, req: SolicitacaoInternaRequest): SolicitacaoInternaResponse {
        if (req.quantidade <= 0) throw RequisicaoInvalidaException("Quantidade deve ser maior que zero")

        val filial = filialRepository.findByIdOrNull(filialId)
            ?: throw EntidadeNaoEncontradaException("Filial não encontrada")

        val produto = req.produtoId?.let { id ->
            produtoRepository.findByIdOrNull(parseUuid(id))
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
        val solicitacao = repo.findByIdOrNull(parseUuid(id))
            ?: throw EntidadeNaoEncontradaException("Solicitação interna $id não encontrada")
        if (solicitacao.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Solicitação interna $id não pertence à filial")
        }

        // Gate de máquina de estados: nenhuma mudança escapa daqui sem passar pela regra de
        // transição abaixo — nem status, nem produtoId. É o que impede reabrir uma solicitação
        // já RECEBIDA (double-increment de estoque) ou já CANCELADA.
        val statusAtual = solicitacao.status
        if (isTerminal(statusAtual) && (req.status != null || req.produtoId != null)) {
            throw RequisicaoInvalidaException(
                "Solicitação $id já está em estado terminal ($statusAtual) — não pode mais ser alterada"
            )
        }
        if (req.status != null && !transicaoPermitida(statusAtual, req.status)) {
            throw RequisicaoInvalidaException(
                "Transição de status $statusAtual para ${req.status} não é permitida"
            )
        }

        // vincular produto (pode vir junto com o RECEBIDO ou antes dele)
        req.produtoId?.let { pid ->
            solicitacao.produto = produtoRepository.findByIdOrNull(parseUuid(pid))
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

    private fun isTerminal(status: StatusSolicitacaoInterna): Boolean =
        status == StatusSolicitacaoInterna.RECEBIDO || status == StatusSolicitacaoInterna.CANCELADO

    /**
     * Máquina de estados de SolicitacaoInterna. Único lugar que decide quais transições de
     * status são válidas — nada disso pode ser decidido de novo em outro canto do código.
     *
     * SOLICITADO -> COMPRADO | RECEBIDO | CANCELADO
     * COMPRADO   -> RECEBIDO | CANCELADO
     * RECEBIDO, CANCELADO são terminais: nenhuma transição sai deles.
     * Qualquer outra combinação (incluindo regressões como COMPRADO -> SOLICITADO) é rejeitada.
     */
    private fun transicaoPermitida(atual: StatusSolicitacaoInterna, novo: StatusSolicitacaoInterna): Boolean =
        when (atual) {
            StatusSolicitacaoInterna.SOLICITADO -> novo == StatusSolicitacaoInterna.COMPRADO ||
                novo == StatusSolicitacaoInterna.RECEBIDO ||
                novo == StatusSolicitacaoInterna.CANCELADO
            StatusSolicitacaoInterna.COMPRADO -> novo == StatusSolicitacaoInterna.RECEBIDO ||
                novo == StatusSolicitacaoInterna.CANCELADO
            StatusSolicitacaoInterna.RECEBIDO, StatusSolicitacaoInterna.CANCELADO -> false
        }

    /** UUID malformado vindo do cliente é requisição inválida (4xx), não erro de servidor (500). */
    private fun parseUuid(valor: String): UUID =
        try {
            UUID.fromString(valor)
        } catch (e: IllegalArgumentException) {
            throw RequisicaoInvalidaException("Identificador inválido: $valor")
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

        produtoEstadoService.aplicarDelta(produto.id, solicitacao.quantidade)

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
