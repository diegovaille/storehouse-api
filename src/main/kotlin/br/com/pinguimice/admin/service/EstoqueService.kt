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
import java.text.Normalizer

@Service
class EstoqueService(
    private val materiaPrimaRepository: MateriaPrimaRepository,
    private val embalagemRepository: EmbalagemRepository,
    private val outrosRepository: OutrosRepository,
    private val estoqueGelinhoRepository: EstoqueGelinhoRepository,
    private val saborRepository: SaborRepository,
    private val parametroCalculoService: ParametroCalculoService
) {

    // Funções auxiliares para buscar parâmetros (com cache)
    private fun getUnidadesPorEmbalagemKg() = parametroCalculoService.obterValor(ParametroCalculoService.UNIDADES_POR_EMBALAGEM_KG)
    private fun getTotalPacotesPorCaixa() = parametroCalculoService.obterValor(ParametroCalculoService.TOTAL_PACOTES_POR_CAIXA)
    private fun getTotalUnidadePorKg() = parametroCalculoService.obterValor(ParametroCalculoService.TOTAL_UNIDADE_POR_KG)
    private fun getTotalUnidadeAcucarPorKg() = parametroCalculoService.obterValor(ParametroCalculoService.TOTAL_UNIDADE_ACUCAR_POR_KG)
    private fun getUnidadesPorPacote() = parametroCalculoService.obterValor(ParametroCalculoService.UNIDADES_POR_PACOTE)

    /**
     * Matéria Prima (Insumos)
     */
    @LogCall
    fun listarMateriaPrima(): List<MateriaPrimaResponse> {
        return materiaPrimaRepository.findAll().filter { it.estoqueUnidades > 0 }.map { it.toResponse() }
    }

    /**
     * Cria uma nova matéria prima (insumo).
     */
    @LogCall
    @Transactional
    fun criarMateriaPrima(request: MateriaPrimaRequest): MateriaPrimaResponse {
        val sabor = obterSabor(request.saborId)

        val totalUnidades = calcularTotalUnidadesMateriaPrima(request.tipoEntrada, request.quantidadeEntrada, sabor)

        val precoPorUnidade = if (totalUnidades > 0) {
            // Help me use the tipoEntrada on a case
            calcularValorPorUnidade(request.precoEntrada, request.tipoEntrada, sabor)
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

    private fun calcularValorPorUnidade(
        precoEntrada: BigDecimal,
        tipoEntrada: TipoEntrada,
        sabor: Sabor?
    ): BigDecimal {
        when (tipoEntrada) {
            TipoEntrada.CAIXA -> {
                val totalUnidades = getTotalPacotesPorCaixa() * getUnidadesPorPacote()
                return precoEntrada.divide(BigDecimal(totalUnidades), 4, RoundingMode.HALF_UP)
            }
            TipoEntrada.PACOTE -> {
                return precoEntrada.divide(BigDecimal(getUnidadesPorPacote()), 4, RoundingMode.HALF_UP)
            }
            TipoEntrada.KG -> {
                val unidadesPorKg = if (sabor != null) getTotalUnidadePorKg() else getTotalUnidadeAcucarPorKg()
                return precoEntrada.divide(BigDecimal(unidadesPorKg), 4, RoundingMode.HALF_UP)
            }
        }
    }

    private fun obterSabor(id: UUID?): Sabor? {
        if (id == null) return null
        return saborRepository.findByIdOrNull(id)
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

        val totalUnidades = calcularTotalUnidadesMateriaPrima(request.tipoEntrada, request.quantidadeEntrada, sabor)
        val precoPorUnidade = if (totalUnidades > 0) {
            calcularValorPorUnidade(request.precoEntrada, request.tipoEntrada, sabor)
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
     * Agora considera que quando sabor == null (matéria-prima global como açúcar) o fator é diferente.
     */
    private fun calcularTotalUnidadesMateriaPrima(tipo: TipoEntrada, quantidade: BigDecimal, sabor: Sabor?): Int {
        return when (tipo) {
            TipoEntrada.CAIXA -> (quantidade.toDouble() * getTotalPacotesPorCaixa() * getUnidadesPorPacote()).toInt()
            TipoEntrada.PACOTE -> (quantidade.toDouble() * getUnidadesPorPacote()).toInt()
            TipoEntrada.KG -> {
                val unidadesPorKg = if (sabor != null) getTotalUnidadePorKg() else getTotalUnidadeAcucarPorKg()
                (quantidade.toDouble() * unidadesPorKg).toInt()
            }
        }
    }

    /**
     * Embalagens
     */
    @LogCall
    fun listarEmbalagens(): List<EmbalagemResponse> {
        return embalagemRepository.findAll().filter { it.estoqueUnidades > 0 }.map { it.toResponse() }
    }

    /**
     * Cria uma nova embalagem.
     */
    @LogCall
    @Transactional
    fun criarEmbalagem(request: EmbalagemRequest): EmbalagemResponse {
        val sabor = obterSabor(request.saborId)

        val totalUnidades = (request.quantidadeKg.toDouble() * getUnidadesPorEmbalagemKg()).toInt()

        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoKg.divide(BigDecimal(getUnidadesPorEmbalagemKg()), 4, RoundingMode.HALF_UP)
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

        val totalUnidades = (request.quantidadeKg.toDouble() * getUnidadesPorEmbalagemKg()).toInt()
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoKg.divide(BigDecimal(getUnidadesPorEmbalagemKg()), 4, RoundingMode.HALF_UP)
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
        return outrosRepository.findAll().filter { it.estoqueUnidades > 0 }.map { it.toResponse() }
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

    // Normaliza nomes e detecta sabores que usam açúcar como base (ex: coco, maçã verde)
    private fun normalizeName(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun saborUsaAcucar(sabor: Sabor?): Boolean {
        if (sabor == null) return false
        val nome = normalizeName(sabor.nome)
        if (nome.contains("coco")) return true
        // detectar maçã verde (maca verde sem acento) ou variações
        if (nome.contains("maca") && nome.contains("verde")) return true
        return false
    }

    @LogCall
    @Transactional
    fun deduzirEstoqueParaProducao(sabor: Sabor, quantidadeProduzida: Int) {
        // 1) Insumos
        // Se o sabor usa açúcar, deduz TANTO o açúcar quanto a matéria-prima do sabor
        if (saborUsaAcucar(sabor)) {
            // 1a) Deduzir açúcar (sabor = null)
            val acucar = materiaPrimaRepository.findBySaborIdIsNull()
            require(acucar.isNotEmpty()) {
                throw RequisicaoInvalidaException("Nenhum estoque de açúcar encontrado para o sabor ${sabor.nome}")
            }
            validarDisponivel(acucar.sumOf { it.estoqueUnidades }, quantidadeProduzida) {
                "Estoque insuficiente de açúcar para o sabor ${sabor.nome}. Disponível: $it, Necessário: $quantidadeProduzida"
            }
            deduzirFIFO(
                items = acucar,
                quantidade = quantidadeProduzida,
                getEstoque = { it.estoqueUnidades },
                setEstoque = { item, novo -> item.estoqueUnidades = novo },
                save = { materiaPrimaRepository.save(it) }
            )

            // 1b) Deduzir matéria-prima do próprio sabor (essência, corante, etc.)
            val insumosSabor = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
            require(insumosSabor.isNotEmpty()) {
                throw RequisicaoInvalidaException("Nenhum estoque de insumo encontrado para o sabor ${sabor.nome}")
            }
            validarDisponivel(insumosSabor.sumOf { it.estoqueUnidades }, quantidadeProduzida) {
                "Estoque insuficiente de insumo para o sabor ${sabor.nome}. Disponível: $it, Necessário: $quantidadeProduzida"
            }
            deduzirFIFO(
                items = insumosSabor,
                quantidade = quantidadeProduzida,
                getEstoque = { it.estoqueUnidades },
                setEstoque = { item, novo -> item.estoqueUnidades = novo },
                save = { materiaPrimaRepository.save(it) }
            )
        } else {
            // Sabores normais: apenas matéria-prima do sabor
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
        }

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
        val plasticos = outrosRepository.findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc("Saco Transparente")
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
        // Se o sabor usa açúcar, restaura TANTO o açúcar quanto a matéria-prima do sabor
        if (saborUsaAcucar(sabor)) {
            // 1a) Restaurar açúcar (sabor = null)
            val acucar = materiaPrimaRepository.findBySaborIdIsNull()
            restaurarReverseFIFO(
                items = acucar,
                quantidade = quantidadeProduzida,
                getTotal = { it.totalUnidades },
                getEstoque = { it.estoqueUnidades },
                addEstoque = { item, delta -> item.estoqueUnidades += delta },
                save = { materiaPrimaRepository.save(it) }
            )

            // 1b) Restaurar matéria-prima do próprio sabor
            val insumosSabor = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
            restaurarReverseFIFO(
                items = insumosSabor,
                quantidade = quantidadeProduzida,
                getTotal = { it.totalUnidades },
                getEstoque = { it.estoqueUnidades },
                addEstoque = { item, delta -> item.estoqueUnidades += delta },
                save = { materiaPrimaRepository.save(it) }
            )
        } else {
            // Sabores normais: apenas matéria-prima do sabor
            val insumos = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
            restaurarReverseFIFO(
                items = insumos,
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
        val plasticos = outrosRepository.findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc("Saco Transparente")
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

    @LogCall
    @Transactional
    fun deletarMateriaPrima(id: UUID) {
        val materiaPrima = materiaPrimaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Matéria prima não encontrada")
        materiaPrimaRepository.delete(materiaPrima)
    }

    @LogCall
    @Transactional
    fun deletarEmbalagem(id: UUID) {
        val embalagem = embalagemRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Embalagem não encontrada")
        embalagemRepository.delete(embalagem)
    }

    @LogCall
    @Transactional
    fun atualizarOutros(id: UUID, request: OutrosRequest): OutrosResponse {
        val outros = outrosRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Outros não encontrado")

        val totalUnidades = request.quantidadeEntrada * request.unidadesPorItem
        val precoPorUnidade = if (totalUnidades > 0) {
            request.precoEntrada.divide(BigDecimal(totalUnidades), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val diffUnidades = totalUnidades - outros.totalUnidades
        outros.estoqueUnidades += diffUnidades

        outros.nome = request.nome
        outros.quantidadeEntrada = request.quantidadeEntrada
        outros.precoEntrada = request.precoEntrada
        outros.unidadesPorItem = request.unidadesPorItem
        outros.totalUnidades = totalUnidades
        outros.precoPorUnidade = precoPorUnidade

        return outrosRepository.save(outros).toResponse()
    }

    @LogCall
    @Transactional
    fun deletarOutros(id: UUID) {
        val outros = outrosRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Outros não encontrado")
        outrosRepository.delete(outros)
    }
}
