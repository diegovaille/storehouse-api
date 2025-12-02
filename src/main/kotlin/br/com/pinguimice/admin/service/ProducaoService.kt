package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.Producao
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

    @LogCall
    @Transactional
    fun registrarProducao(request: ProducaoRequest): ProducaoResponse {
        val sabor = saborRepository.findByIdOrNull(request.saborId)
            ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

        // Auto-deduct stock if requested
        if (request.deduzirEstoque) {
            estoqueService.deduzirEstoqueParaProducao(sabor, request.quantidadeProduzida)
        }

        // Create production record
        val producao = Producao(
            sabor = sabor,
            quantidadeProduzida = request.quantidadeProduzida,
            deduzirEstoque = request.deduzirEstoque,
            dataProducao = request.dataProducao ?: LocalDateTime.now(),
            observacoes = request.observacoes
        )

        // Update Estoque Gelinho (always update stock of finished product)
        estoqueService.atualizarEstoqueGelinho(sabor, request.quantidadeProduzida)

        return producaoRepository.save(producao).toResponse()
    }

    @LogCall
    fun listarProducao(inicio: String?, fim: String?): List<ProducaoResponse> {
        val producoes = if (inicio != null && fim != null) {
            val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
            val dataFim = LocalDateTime.parse("${fim}T23:59:59")
            producaoRepository.findByDataProducaoBetweenOrderByDataProducaoDesc(dataInicio, dataFim)
        } else {
            producaoRepository.findAllByOrderByDataProducaoDesc()
        }

        return producoes.map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun excluirProducao(id: UUID) {
        val producao = producaoRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Produção não encontrada")
        
        // Reverse stock deduction if it was deducted
        if (producao.deduzirEstoque) {
            estoqueService.reverterDeducaoEstoque(producao.sabor, producao.quantidadeProduzida)
        }
        
        // Decrease Estoque Gelinho (reverse the addition)
        estoqueService.atualizarEstoqueGelinho(producao.sabor, -producao.quantidadeProduzida)
        
        producaoRepository.delete(producao)
    }
}
