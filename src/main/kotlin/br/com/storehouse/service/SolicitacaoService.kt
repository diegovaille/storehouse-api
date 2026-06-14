package br.com.storehouse.service

import br.com.storehouse.data.entities.Solicitacao
import br.com.storehouse.data.enums.StatusSolicitacao
import br.com.storehouse.data.model.SolicitacaoRequest
import br.com.storehouse.data.model.SolicitacaoResponse
import br.com.storehouse.data.model.SolicitacaoUpdateRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.SolicitacaoRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class SolicitacaoService(
    private val repo: SolicitacaoRepository,
    private val filialRepository: FilialRepository
) {
    @Transactional
    fun criar(filialId: UUID, req: SolicitacaoRequest): SolicitacaoResponse {
        val filial = filialRepository.findByIdOrNull(filialId)
            ?: throw EntidadeNaoEncontradaException("Filial não encontrada")
        val solicitacao = Solicitacao(
            filial = filial,
            descricaoItem = req.descricaoItem,
            categoria = req.categoria,
            nomeSolicitante = req.nomeSolicitante,
            contato = req.contato,
            observacao = req.observacao
        )
        return repo.save(solicitacao).toResponse()
    }

    fun listar(filialId: UUID, status: StatusSolicitacao?): List<SolicitacaoResponse> {
        val lista = if (status != null) {
            repo.findByFilialIdAndStatusInOrderByDataCriacaoDesc(filialId, listOf(status))
        } else {
            // ativas: tudo menos RETIRADO/CANCELADO
            repo.findByFilialIdAndStatusInOrderByDataCriacaoDesc(
                filialId, listOf(StatusSolicitacao.SOLICITADO, StatusSolicitacao.SEPARADO)
            )
        }
        return lista.map { it.toResponse() }
    }

    @Transactional
    fun atualizar(filialId: UUID, id: String, req: SolicitacaoUpdateRequest): SolicitacaoResponse {
        val solicitacao = repo.findByIdOrNull(UUID.fromString(id))
            ?: throw EntidadeNaoEncontradaException("Solicitação $id não encontrada")
        if (solicitacao.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Solicitação $id não pertence à filial")
        }
        if (req.status != null) solicitacao.status = req.status
        if (req.notificar) solicitacao.notificadoEm = LocalDateTime.now()
        solicitacao.dataAtualizacao = LocalDateTime.now()
        return repo.save(solicitacao).toResponse()
    }
}

fun Solicitacao.toResponse() = SolicitacaoResponse(
    id = this.id.toString(),
    descricaoItem = this.descricaoItem,
    categoria = this.categoria,
    nomeSolicitante = this.nomeSolicitante,
    contato = this.contato,
    observacao = this.observacao,
    status = this.status,
    dataCriacao = this.dataCriacao.toString(),
    dataAtualizacao = this.dataAtualizacao?.toString(),
    notificadoEm = this.notificadoEm?.toString()
)
