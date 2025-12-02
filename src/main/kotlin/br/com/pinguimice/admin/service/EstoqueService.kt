package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.*
import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.repository.*
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil

private const val UNIDADES_POR_EMBALAGEM_KG = 700
private const val TOTAL_PACOTES_POR_CAIXA = 480
private const val TOTAL_UNIDADE_POR_KG = 266
private const val UNIDADES_POR_PACOTE = 4.4

@Service
class EstoqueService(
    private val materiaPrimaRepository: MateriaPrimaRepository,
    private val embalagemRepository: EmbalagemRepository,
    private val outrosRepository: OutrosRepository,
    private val estoqueGelinhoRepository: EstoqueGelinhoRepository,
    private val saborRepository: SaborRepository
) {

    // ==================== MATÉRIA PRIMA (INSUMOS) ====================
    
    @LogCall
    fun listarMateriaPrima(): List<MateriaPrimaResponse> {
        return materiaPrimaRepository.findAll().map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun criarMateriaPrima(request: MateriaPrimaRequest): MateriaPrimaResponse {
        val sabor = request.saborId?.let { 
            saborRepository.findByIdOrNull(it) ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
        }

        val totalUnidades = calcularTotalUnidadesMateriaPrima(request.tipoEntrada, request.quantidadeEntrada)
        
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoEntrada.divide(BigDecimal(totalUnidades), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val materiaPrima = MateriaPrima(
            nome = request.nome,
            sabor = sabor,
            tipoEntrada = request.tipoEntrada.name,
            quantidadeEntrada = request.quantidadeEntrada,
            precoEntrada = request.precoEntrada,
            totalUnidades = totalUnidades,
            precoPorUnidade = precoPorUnidade,
            estoqueUnidades = totalUnidades
        )

        return materiaPrimaRepository.save(materiaPrima).toResponse()
    }

    @LogCall
    @Transactional
    fun atualizarMateriaPrima(id: UUID, request: MateriaPrimaRequest): MateriaPrimaResponse {
        val materiaPrima = materiaPrimaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Matéria prima não encontrada")

        val sabor = request.saborId?.let { 
            saborRepository.findByIdOrNull(it) ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
        }

        val totalUnidades = calcularTotalUnidadesMateriaPrima(request.tipoEntrada, request.quantidadeEntrada)
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoEntrada.divide(BigDecimal(totalUnidades), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val diffUnidades = totalUnidades - materiaPrima.totalUnidades
        materiaPrima.estoqueUnidades += diffUnidades

        materiaPrima.nome = request.nome
        materiaPrima.sabor = sabor
        materiaPrima.tipoEntrada = request.tipoEntrada.name
        materiaPrima.quantidadeEntrada = request.quantidadeEntrada
        materiaPrima.precoEntrada = request.precoEntrada
        materiaPrima.totalUnidades = totalUnidades
        materiaPrima.precoPorUnidade = precoPorUnidade

        return materiaPrimaRepository.save(materiaPrima).toResponse()
    }

    private fun calcularTotalUnidadesMateriaPrima(tipo: TipoEntrada, quantidade: BigDecimal): Int {
        return when (tipo) {
            TipoEntrada.CAIXA -> (quantidade.toDouble() * TOTAL_PACOTES_POR_CAIXA * UNIDADES_POR_PACOTE).toInt()
            TipoEntrada.PACOTE -> (quantidade.toDouble() * UNIDADES_POR_PACOTE).toInt()
            TipoEntrada.KG -> (quantidade.toDouble() * TOTAL_UNIDADE_POR_KG).toInt()
        }
    }

    // ==================== EMBALAGEM ====================

    @LogCall
    fun listarEmbalagens(): List<EmbalagemResponse> {
        return embalagemRepository.findAll().map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun criarEmbalagem(request: EmbalagemRequest): EmbalagemResponse {
        val sabor = request.saborId?.let { 
            saborRepository.findByIdOrNull(it) ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
        }

        val totalUnidades = (request.quantidadeKg.toDouble() * UNIDADES_POR_EMBALAGEM_KG).toInt()
        
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoKg.divide(BigDecimal(UNIDADES_POR_EMBALAGEM_KG), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val embalagem = Embalagem(
            nome = request.nome,
            sabor = sabor,
            quantidadeKg = request.quantidadeKg,
            precoKg = request.precoKg,
            totalUnidades = totalUnidades,
            precoPorUnidade = precoPorUnidade,
            estoqueUnidades = totalUnidades
        )

        return embalagemRepository.save(embalagem).toResponse()
    }

    @LogCall
    @Transactional
    fun atualizarEmbalagem(id: UUID, request: EmbalagemRequest): EmbalagemResponse {
        val embalagem = embalagemRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Embalagem não encontrada")

        val sabor = request.saborId?.let { 
            saborRepository.findByIdOrNull(it) ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
        }

        val totalUnidades = (request.quantidadeKg.toDouble() * UNIDADES_POR_EMBALAGEM_KG).toInt()
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoKg.divide(BigDecimal(UNIDADES_POR_EMBALAGEM_KG), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val diffUnidades = totalUnidades - embalagem.totalUnidades
        embalagem.estoqueUnidades += diffUnidades

        embalagem.nome = request.nome
        embalagem.sabor = sabor
        embalagem.quantidadeKg = request.quantidadeKg
        embalagem.precoKg = request.precoKg
        embalagem.totalUnidades = totalUnidades
        embalagem.precoPorUnidade = precoPorUnidade

        return embalagemRepository.save(embalagem).toResponse()
    }

    // ==================== OUTROS ====================

    @LogCall
    fun listarOutros(): List<OutrosResponse> {
        return outrosRepository.findAll().map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun criarOutros(request: OutrosRequest): OutrosResponse {
        val totalUnidades = request.quantidadeEntrada * request.unidadesPorItem
        
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoEntrada.divide(BigDecimal(totalUnidades), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val outros = Outros(
            nome = request.nome,
            quantidadeEntrada = request.quantidadeEntrada,
            precoEntrada = request.precoEntrada,
            unidadesPorItem = request.unidadesPorItem,
            totalUnidades = totalUnidades,
            precoPorUnidade = precoPorUnidade,
            estoqueUnidades = totalUnidades
        )

        return outrosRepository.save(outros).toResponse()
    }

    // ==================== ESTOQUE GELINHO ====================

    @LogCall
    fun listarEstoqueGelinho(): List<EstoqueGelinhoResponse> {
        return estoqueGelinhoRepository.findAll().map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun atualizarEstoqueGelinho(sabor: Sabor, quantidade: Int) {
        val estoqueGelinho = estoqueGelinhoRepository.findBySaborId(sabor.id)
        if (estoqueGelinho != null) {
            estoqueGelinho.quantidade += quantidade
            estoqueGelinho.ultimaAtualizacao = LocalDateTime.now()
            estoqueGelinhoRepository.save(estoqueGelinho)
        } else {
            val novoEstoque = EstoqueGelinho(
                sabor = sabor,
                quantidade = quantidade
            )
            estoqueGelinhoRepository.save(novoEstoque)
        }
    }

    // ==================== DEDUÇÃO DE ESTOQUE ====================

    @LogCall
    @Transactional
    fun deduzirEstoqueParaProducao(sabor: Sabor, quantidadeProduzida: Int) {
        // 1. Deduct Insumos (by flavor) - FIFO
        val insumos = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        if (insumos.isEmpty()) {
             throw RequisicaoInvalidaException("Nenhum estoque de insumo encontrado para o sabor ${sabor.nome}")
        }
        
        var quantidadeRestanteInsumo = quantidadeProduzida
        
        // Check total stock first
        val totalEstoqueInsumo = insumos.sumOf { it.estoqueUnidades }
        if (totalEstoqueInsumo < quantidadeProduzida) {
            throw RequisicaoInvalidaException("Estoque insuficiente de insumo para o sabor ${sabor.nome}. Disponível: $totalEstoqueInsumo, Necessário: $quantidadeProduzida")
        }

        for (insumo in insumos) {
            if (quantidadeRestanteInsumo <= 0) break
            
            if (insumo.estoqueUnidades >= quantidadeRestanteInsumo) {
                insumo.estoqueUnidades -= quantidadeRestanteInsumo
                quantidadeRestanteInsumo = 0
            } else {
                quantidadeRestanteInsumo -= insumo.estoqueUnidades
                insumo.estoqueUnidades = 0
            }
            materiaPrimaRepository.save(insumo)
        }

        // 2. Deduct Embalagem (by flavor) - FIFO
        val embalagens = embalagemRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        if (embalagens.isEmpty()) {
             throw RequisicaoInvalidaException("Nenhum estoque de embalagem encontrado para o sabor ${sabor.nome}")
        }

        var quantidadeRestanteEmbalagem = quantidadeProduzida
        
        val totalEstoqueEmbalagem = embalagens.sumOf { it.estoqueUnidades }
        if (totalEstoqueEmbalagem < quantidadeProduzida) {
            throw RequisicaoInvalidaException("Estoque insuficiente de embalagem para o sabor ${sabor.nome}. Disponível: $totalEstoqueEmbalagem, Necessário: $quantidadeProduzida")
        }

        for (embalagem in embalagens) {
            if (quantidadeRestanteEmbalagem <= 0) break
            
            if (embalagem.estoqueUnidades >= quantidadeRestanteEmbalagem) {
                embalagem.estoqueUnidades -= quantidadeRestanteEmbalagem
                quantidadeRestanteEmbalagem = 0
            } else {
                quantidadeRestanteEmbalagem -= embalagem.estoqueUnidades
                embalagem.estoqueUnidades = 0
            }
            embalagemRepository.save(embalagem)
        }

        // 3. Deduct Outros (Plastics - 1 per 50 units) - FIFO
        val plasticosNecessarios = ceil(quantidadeProduzida / 50.0).toInt() * 50
        
        // Find "Plástico" items
        val plasticos = outrosRepository.findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc("Plástico")
        if (plasticos.isEmpty()) {
             // If no plastic found, maybe we shouldn't block? Or throw?
             // Assuming strict control:
             throw RequisicaoInvalidaException("Nenhum estoque de plástico encontrado")
        }

        var quantidadeRestantePlastico = plasticosNecessarios
        
        val totalEstoquePlastico = plasticos.sumOf { it.estoqueUnidades }
        if (totalEstoquePlastico < plasticosNecessarios) {
             throw RequisicaoInvalidaException("Estoque insuficiente de plásticos. Disponível: $totalEstoquePlastico, Necessário: $plasticosNecessarios")
        }

        for (plastico in plasticos) {
            if (quantidadeRestantePlastico <= 0) break
            
            if (plastico.estoqueUnidades >= quantidadeRestantePlastico) {
                plastico.estoqueUnidades -= quantidadeRestantePlastico
                quantidadeRestantePlastico = 0
            } else {
                quantidadeRestantePlastico -= plastico.estoqueUnidades
                plastico.estoqueUnidades = 0
            }
            outrosRepository.save(plastico)
        }
    }

    @LogCall
    @Transactional
    fun reverterDeducaoEstoque(sabor: Sabor, quantidadeProduzida: Int) {
        // 1. Restore Insumos (by flavor) - Add back to the most recent batches (reverse FIFO)
        val insumos = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        if (insumos.isNotEmpty()) {
            var quantidadeRestanteInsumo = quantidadeProduzida
            
            // Add back to the most recent batches first (reverse order)
            for (insumo in insumos.reversed()) {
                if (quantidadeRestanteInsumo <= 0) break
                
                val quantidadeARestaurar = minOf(quantidadeRestanteInsumo, insumo.totalUnidades - insumo.estoqueUnidades)
                insumo.estoqueUnidades += quantidadeARestaurar
                quantidadeRestanteInsumo -= quantidadeARestaurar
                
                materiaPrimaRepository.save(insumo)
            }
        }

        // 2. Restore Embalagem (by flavor)
        val embalagens = embalagemRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        if (embalagens.isNotEmpty()) {
            var quantidadeRestanteEmbalagem = quantidadeProduzida
            
            for (embalagem in embalagens.reversed()) {
                if (quantidadeRestanteEmbalagem <= 0) break
                
                val quantidadeARestaurar = minOf(quantidadeRestanteEmbalagem, embalagem.totalUnidades - embalagem.estoqueUnidades)
                embalagem.estoqueUnidades += quantidadeARestaurar
                quantidadeRestanteEmbalagem -= quantidadeARestaurar
                
                embalagemRepository.save(embalagem)
            }
        }

        // 3. Restore Outros (Plastics - 1 per 50 units)
        val plasticosNecessarios = ceil(quantidadeProduzida / 50.0).toInt() * 50
        val plasticos = outrosRepository.findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc("Plástico")
        
        if (plasticos.isNotEmpty()) {
            var quantidadeRestantePlastico = plasticosNecessarios
            
            for (plastico in plasticos.reversed()) {
                if (quantidadeRestantePlastico <= 0) break
                
                val quantidadeARestaurar = minOf(quantidadeRestantePlastico, plastico.totalUnidades - plastico.estoqueUnidades)
                plastico.estoqueUnidades += quantidadeARestaurar
                quantidadeRestantePlastico -= quantidadeARestaurar
                
                outrosRepository.save(plastico)
            }
        }
    }
}
