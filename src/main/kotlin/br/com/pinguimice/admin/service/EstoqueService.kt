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

    /**
     * Matéria Prima (Insumos)
     */
    @LogCall
    fun listarMateriaPrima(): List<MateriaPrimaResponse> {
        return materiaPrimaRepository.findAll().map { it.toResponse() }
    }

    /**
     * Cria uma nova matéria prima (insumo).
     */
    @LogCall
    @Transactional
    fun criarMateriaPrima(request: MateriaPrimaRequest): MateriaPrimaResponse {
        val sabor = obterSabor(request.saborId)

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

    private fun obterSabor(id: UUID?): Sabor {
        return saborRepository.findByIdOrNull(id) ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
    }

    /**
     * Atualiza uma matéria prima (insumo) existente.
     */
    @LogCall
    @Transactional
    fun atualizarMateriaPrima(id: UUID, request: MateriaPrimaRequest): MateriaPrimaResponse {
        val materiaPrima = materiaPrimaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Matéria prima não encontrada")

        val sabor = obterSabor(request.saborId)

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

    /**
     * Calcula o total de unidades de matéria prima com base no tipo de entrada e quantidade.
     */
    private fun calcularTotalUnidadesMateriaPrima(tipo: TipoEntrada, quantidade: BigDecimal): Int {
        return when (tipo) {
            TipoEntrada.CAIXA -> (quantidade.toDouble() * TOTAL_PACOTES_POR_CAIXA * UNIDADES_POR_PACOTE).toInt()
            TipoEntrada.PACOTE -> (quantidade.toDouble() * UNIDADES_POR_PACOTE).toInt()
            TipoEntrada.KG -> (quantidade.toDouble() * TOTAL_UNIDADE_POR_KG).toInt()
        }
    }

    /**
     * Embalagens
     */
    @LogCall
    fun listarEmbalagens(): List<EmbalagemResponse> {
        return embalagemRepository.findAll().map { it.toResponse() }
    }

    /**
     * Cria uma nova embalagem.
     */
    @LogCall
    @Transactional
    fun criarEmbalagem(request: EmbalagemRequest): EmbalagemResponse {
        val sabor = obterSabor(request.saborId)

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

    /**
     * Atualiza uma embalagem existente.
     */
    @LogCall
    @Transactional
    fun atualizarEmbalagem(id: UUID, request: EmbalagemRequest): EmbalagemResponse {
        val embalagem = embalagemRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Embalagem não encontrada")

        val sabor = obterSabor(id = request.saborId)

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

    /**
     * Outros
     */
    @LogCall
    fun listarOutros(): List<OutrosResponse> {
        return outrosRepository.findAll().map { it.toResponse() }
    }

    /**
     * Cria um novo item de outros.
     */
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

    /**
     * Atualiza um item de outros existente.
     */
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
        // 1) Insumos
        val insumos = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        require(insumos.isNotEmpty()) {
            throw RequisicaoInvalidaException("Nenhum estoque de insumo encontrado para o sabor ${sabor.nome}")
        }
        validarDisponivel(insumos.sumOf { it.estoqueUnidades }, quantidadeProduzida) {
            "Estoque insuficiente de insumo para o sabor ${sabor.nome}. Disponível: $it, Necessário: $quantidadeProduzida"
        }
        deduzirFIFO(
            items = insumos,
            quantidade = quantidadeProduzida,
            getEstoque = { it.estoqueUnidades },
            setEstoque = { item, novo -> item.estoqueUnidades = novo },
            save = { materiaPrimaRepository.save(it) }
        )

        // 2) Embalagens
        val embalagens = embalagemRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        require(embalagens.isNotEmpty()) {
            throw RequisicaoInvalidaException("Nenhum estoque de embalagem encontrado para o sabor ${sabor.nome}")
        }
        validarDisponivel(embalagens.sumOf { it.estoqueUnidades }, quantidadeProduzida) {
            "Estoque insuficiente de embalagem para o sabor ${sabor.nome}. Disponível: $it, Necessário: $quantidadeProduzida"
        }
        deduzirFIFO(
            items = embalagens,
            quantidade = quantidadeProduzida,
            getEstoque = { it.estoqueUnidades },
            setEstoque = { item, novo -> item.estoqueUnidades = novo },
            save = { embalagemRepository.save(it) }
        )

        // 3) Plásticos (1 por 50 unidades \- arredonda para múltiplos de 50)
        val plasticosNecessarios = ceil(quantidadeProduzida / 50.0).toInt() * 50
        val plasticos = outrosRepository.findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc("Plástico")
        require(plasticos.isNotEmpty()) {
            throw RequisicaoInvalidaException("Nenhum estoque de plástico encontrado")
        }
        validarDisponivel(plasticos.sumOf { it.estoqueUnidades }, plasticosNecessarios) {
            "Estoque insuficiente de plásticos. Disponível: $it, Necessário: $plasticosNecessarios"
        }
        deduzirFIFO(
            items = plasticos,
            quantidade = plasticosNecessarios,
            getEstoque = { it.estoqueUnidades },
            setEstoque = { item, novo -> item.estoqueUnidades = novo },
            save = { outrosRepository.save(it) }
        )
    }

    @LogCall
    @Transactional
    fun reverterDeducaoEstoque(sabor: Sabor, quantidadeProduzida: Int) {
        // 1) Insumos
        val insumos = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id).apply {
            restaurarReverseFIFO(
                items = this,
                quantidade = quantidadeProduzida,
                getTotal = { it.totalUnidades },
                getEstoque = { it.estoqueUnidades },
                addEstoque = { item, delta -> item.estoqueUnidades += delta },
                save = { materiaPrimaRepository.save(it) }
            )
        }

        // 2) Embalagens
        val embalagens = embalagemRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        restaurarReverseFIFO(
            items = embalagens,
            quantidade = quantidadeProduzida,
            getTotal = { it.totalUnidades },
            getEstoque = { it.estoqueUnidades },
            addEstoque = { item, delta -> item.estoqueUnidades += delta },
            save = { embalagemRepository.save(it) }
        )

        // 3) Plásticos
        val plasticosNecessarios = ceil(quantidadeProduzida / 50.0).toInt() * 50
        val plasticos = outrosRepository.findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc("Plástico")
        restaurarReverseFIFO(
            items = plasticos,
            quantidade = plasticosNecessarios,
            getTotal = { it.totalUnidades },
            getEstoque = { it.estoqueUnidades },
            addEstoque = { item, delta -> item.estoqueUnidades += delta },
            save = { outrosRepository.save(it) }
        )
    }

    private fun validarDisponivel(disponivel: Int, necessario: Int, msg: (Int) -> String) {
        if (disponivel < necessario) throw RequisicaoInvalidaException(msg(disponivel))
    }

    /**
     * Restaura em Reverse FIFO: preenche dos mais novos para os mais antigos,
     * sem ultrapassar `totalUnidades`.
     */
    private fun <T> restaurarReverseFIFO(
        items: List<T>,
        quantidade: Int,
        getTotal: (T) -> Int,
        getEstoque: (T) -> Int,
        addEstoque: (T, Int) -> Unit,
        save: (T) -> Unit
    ) {
        var restante = quantidade
        for (item in items.asReversed()) {
            if (restante <= 0) break
            val capacidade = getTotal(item) - getEstoque(item)
            if (capacidade <= 0) continue
            val aRestaurar = minOf(restante, capacidade)
            addEstoque(item, aRestaurar)
            restante -= aRestaurar
            save(item)
        }
    }

    /**
     * Deduz em FIFO: consome dos mais antigos para os mais novos.
     */
    private fun <T> deduzirFIFO(
        items: List<T>,
        quantidade: Int,
        getEstoque: (T) -> Int,
        setEstoque: (T, Int) -> Unit,
        save: (T) -> Unit
    ) {
        var restante = quantidade
        for (item in items) {
            if (restante <= 0) break
            val estoque = getEstoque(item)
            if (estoque >= restante) {
                setEstoque(item, estoque - restante)
                restante = 0
            } else {
                setEstoque(item, 0)
                restante -= estoque
            }
            save(item)
        }
    }
}
