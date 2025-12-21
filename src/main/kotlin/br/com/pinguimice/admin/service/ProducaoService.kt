package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.Producao
import br.com.pinguimice.admin.entity.Sabor
import br.com.pinguimice.admin.model.ProducaoRequest
import br.com.pinguimice.admin.model.ProducaoResponse
import br.com.pinguimice.admin.repository.ProducaoRepository
import br.com.pinguimice.admin.repository.SaborRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class ProducaoService(
    private val producaoRepository: ProducaoRepository,
    private val saborRepository: SaborRepository,
    private val estoqueService: EstoqueService
) {

    private fun obterSabor(id: UUID, filialId: UUID): Sabor {
        val sabor = saborRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
        if (sabor.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Sabor não pertence à filial informada")
        }
        return sabor
    }

    @LogCall
    @Transactional
    fun registrarProducao(request: ProducaoRequest, filialId: UUID): ProducaoResponse {
        val sabor = obterSabor(request.saborId, filialId)

        // Auto-deduct stock if requested
        if (request.deduzirEstoque) {
            estoqueService.deduzirEstoqueParaProducao(sabor, request.quantidadeProduzida, filialId)
        }

        // Create production record
        val producao = Producao(
            sabor = sabor,
            quantidadeProduzida = request.quantidadeProduzida,
            deduzirEstoque = request.deduzirEstoque,
            dataProducao = request.dataProducao ?: LocalDateTime.now(),
            filial = sabor.filial,
            observacoes = request.observacoes
        )

        // Update Estoque Gelinho (always update stock of finished product)
        estoqueService.atualizarEstoqueGelinho(sabor, request.quantidadeProduzida, filialId)

        return producaoRepository.save(producao).toResponse()
    }

    @LogCall
    fun listarProducao(inicio: String?, fim: String?, filialId: UUID): List<ProducaoResponse> {
        val producoes = if (inicio != null && fim != null) {
            val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
            val dataFim = LocalDateTime.parse("${fim}T23:59:59")
            producaoRepository.findByFilialIdAndDataProducaoBetweenOrderByDataProducaoDesc(filialId, dataInicio, dataFim)
        } else {
            producaoRepository.findAllByFilialIdOrderByDataProducaoDesc(filialId)
        }

        return producoes.map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun excluirProducao(id: UUID, filialId: UUID) {
        val producao = producaoRepository.findByIdAndFilialId(id, filialId)
            ?: throw EntidadeNaoEncontradaException("Produção não encontrada")

        // Reverse stock deduction if it was deducted
        if (producao.deduzirEstoque) {
            estoqueService.reverterDeducaoEstoque(producao.sabor, producao.quantidadeProduzida, filialId)
        }

        // Decrease Estoque Gelinho (reverse the addition)
        estoqueService.atualizarEstoqueGelinho(producao.sabor, -producao.quantidadeProduzida, filialId)

        producaoRepository.delete(producao)
    }
}
