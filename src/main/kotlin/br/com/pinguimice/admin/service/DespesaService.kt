package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.Despesa
import br.com.pinguimice.admin.model.DespesaRequest
import br.com.pinguimice.admin.model.DespesaResponse
import br.com.pinguimice.admin.repository.DespesaRepository
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class DespesaService(
    private val despesaRepository: DespesaRepository,
    private val storageService: br.com.storehouse.service.StorageService,
    private val filialRepository: FilialRepository
) {

    @LogCall
    fun listarDespesas(inicio: String?, fim: String?, filialId: UUID): List<DespesaResponse> {
        val despesas = if (inicio != null && fim != null) {
            val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
            val dataFim = LocalDateTime.parse("${fim}T23:59:59")
            despesaRepository.findByFilialIdAndDataCriacaoBetweenOrderByDataCriacaoDesc(filialId, dataInicio, dataFim)
        } else {
            despesaRepository.findAllByFilialIdOrderByDataCriacaoDesc(filialId)
        }

        return despesas.map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun criarDespesa(
        request: DespesaRequest,
        filialId: UUID,
        arquivo: ByteArray? = null,
        nomeArquivo: String? = null,
        contentType: String? = null
    ): DespesaResponse {
        val filial = filialRepository.findById(filialId)
            .orElseThrow { EntidadeNaoEncontradaException("Filial não encontrada") }

        val despesaId = UUID.randomUUID()

        val anexoUrl = if (arquivo != null && nomeArquivo != null && contentType != null) {
            storageService.uploadAnexoDespesa(despesaId, nomeArquivo, arquivo, contentType)
        } else {
            request.anexoUrl
        }

        val despesa = Despesa(
            id = despesaId,
            descricao = request.descricao,
            valor = request.valor,
            dataVencimento = request.dataVencimento?.let { LocalDate.parse(it) },
            dataPagamento = request.dataPagamento?.let { LocalDate.parse(it) },
            anexoUrl = anexoUrl,
            observacao = request.observacao,
            filial = filial
        )

        return despesaRepository.save(despesa).toResponse()
    }

    @LogCall
    @Transactional
    fun atualizarDespesa(
        id: UUID,
        request: DespesaRequest,
        filialId: UUID,
        arquivo: ByteArray? = null,
        nomeArquivo: String? = null,
        contentType: String? = null
    ): DespesaResponse {
        val despesa = despesaRepository.findByIdAndFilialId(id, filialId)
            ?: throw EntidadeNaoEncontradaException("Despesa não encontrada")

        val anexoUrl = if (arquivo != null && nomeArquivo != null && contentType != null) {
            storageService.uploadAnexoDespesa(id, nomeArquivo, arquivo, contentType)
        } else {
            request.anexoUrl ?: despesa.anexoUrl
        }

        despesa.descricao = request.descricao
        despesa.valor = request.valor
        despesa.dataVencimento = request.dataVencimento?.let { LocalDate.parse(it) }
        despesa.dataPagamento = request.dataPagamento?.let { LocalDate.parse(it) }
        despesa.anexoUrl = anexoUrl
        despesa.observacao = request.observacao

        return despesaRepository.save(despesa).toResponse()
    }

    @LogCall
    @Transactional
    fun deletarDespesa(id: UUID, filialId: UUID) {
        val despesa = despesaRepository.findByIdAndFilialId(id, filialId)
            ?: throw EntidadeNaoEncontradaException("Despesa não encontrada")

        despesaRepository.delete(despesa)
    }
}
