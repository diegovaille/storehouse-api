package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.repository.*
import br.com.storehouse.logging.LogCall
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class PinguimRelatorioService(
    private val despesaRepository: DespesaRepository,
    private val vendaRepository: PinguimVendaRepository,
    private val producaoRepository: ProducaoRepository,
    private val materiaPrimaRepository: MateriaPrimaRepository,
    private val embalagemRepository: EmbalagemRepository,
    private val estoqueGelinhoRepository: EstoqueGelinhoRepository
) {

    // ==================== Relatório de Despesas ====================
    @LogCall
    fun gerarRelatorioDespesas(inicio: String, fim: String, filialId: UUID): RelatorioDespesasResponse {
        val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
        val dataFim = LocalDateTime.parse("${fim}T23:59:59")

        val despesas = despesaRepository.findByFilialIdAndDataCriacaoBetweenOrderByDataCriacaoDesc(filialId, dataInicio, dataFim)

        val totalDespesas = despesas.sumOf { it.valor }
        val totalPago = despesas.filter { it.dataPagamento != null }.sumOf { it.valor }
        val totalPendente = totalDespesas - totalPago

        // Agrupa despesas por categoria (primeira palavra da descrição)
        val despesasPorCategoria = despesas
            .groupBy { it.descricao.split(" ").first().uppercase() }
            .map { (categoria, lista) ->
                val valor = lista.sumOf { it.valor }
                DespesaPorCategoriaItem(
                    categoria = categoria,
                    valor = valor,
                    percentual = if (totalDespesas > BigDecimal.ZERO)
                        (valor.divide(totalDespesas, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toDouble()
                    else 0.0,
                    quantidade = lista.size
                )
            }
            .sortedByDescending { it.valor }

        // Agrupa despesas por semana (ISO Week)
        val despesasPorSemana = despesas
            .groupBy {
                val date = it.dataCriacao.toLocalDate()
                val weekYear = date.year
                val weekNum = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
                String.format("%04d-W%02d", weekYear, weekNum)
            }
            .map { (semana, lista) ->
                DespesaPorPeriodoItem(
                    periodo = semana,
                    valor = lista.sumOf { it.valor },
                    quantidade = lista.size
                )
            }
            .sortedBy { it.periodo }

        // Agrupa despesas por mês
        val despesasPorMes = despesas
            .groupBy { it.dataCriacao.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
            .map { (mes, lista) ->
                DespesaPorPeriodoItem(
                    periodo = mes,
                    valor = lista.sumOf { it.valor },
                    quantidade = lista.size
                )
            }
            .sortedBy { it.periodo }

        // Despesas detalhadas
        val despesasDetalhadas = despesas.take(50).map {
            val dataVenc = it.dataVencimento
            val status = when {
                it.dataPagamento != null -> "PAGO"
                dataVenc != null && dataVenc.isBefore(LocalDate.now()) -> "VENCIDO"
                else -> "PENDENTE"
            }
            DespesaDetalhadaItem(
                descricao = it.descricao,
                valor = it.valor,
                dataPagamento = it.dataPagamento?.toString(),
                dataVencimento = dataVenc?.toString(),
                status = status
            )
        }

        return RelatorioDespesasResponse(
            periodo = PeriodoRelatorio(inicio, fim),
            totalDespesas = totalDespesas,
            totalPago = totalPago,
            totalPendente = totalPendente,
            despesasPorCategoria = despesasPorCategoria,
            despesasPorSemana = despesasPorSemana,
            despesasPorMes = despesasPorMes,
            despesasDetalhadas = despesasDetalhadas
        )
    }

    // ==================== Relatório de Vendas ====================
    @LogCall
    fun gerarRelatorioVendas(inicio: String, fim: String, filialId: UUID): RelatorioVendasResponse {
        val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
        val dataFim = LocalDateTime.parse("${fim}T23:59:59")

        val vendas = vendaRepository.findByFilialIdAndDataVendaBetweenOrderByDataVendaDesc(filialId, dataInicio, dataFim)

        val totalVendas = vendas.sumOf { it.total }
        val totalRecebido = vendas.sumOf { it.totalPago }
        val totalPendente = totalVendas - totalRecebido
        val quantidadeVendas = vendas.size
        val ticketMedio = if (quantidadeVendas > 0)
            totalVendas.divide(BigDecimal(quantidadeVendas), 2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO

        // Vendas por sabor
        val todosSabores = vendas.flatMap { it.itens }
        val totalQuantidadeSabores = todosSabores.sumOf { it.quantidade }
        val vendasPorSabor = todosSabores
            .groupBy { it.sabor.nome }
            .map { (sabor, itens) ->
                val quantidade = itens.sumOf { it.quantidade }
                val valorTotal = itens.sumOf {
                    val vendaItem = it
                    val venda = vendas.first { v -> v.itens.contains(vendaItem) }
                    // Calcula valor proporcional do item na venda
                    val totalItensVenda = venda.itens.sumOf { item -> item.quantidade }
                    if (totalItensVenda > 0) {
                        venda.total.multiply(BigDecimal(it.quantidade))
                            .divide(BigDecimal(totalItensVenda), 2, RoundingMode.HALF_UP)
                    } else BigDecimal.ZERO
                }
                VendaPorSaborItem(
                    sabor = sabor,
                    quantidade = quantidade,
                    valorTotal = valorTotal,
                    percentualQuantidade = if (totalQuantidadeSabores > 0)
                        (quantidade.toDouble() / totalQuantidadeSabores * 100) else 0.0,
                    percentualValor = if (totalVendas > BigDecimal.ZERO)
                        (valorTotal.divide(totalVendas, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toDouble()
                    else 0.0
                )
            }
            .sortedByDescending { it.quantidade }

        // Vendas por região (derivada do cliente, quando existir)
        val vendasPorRegiao = vendas
            .groupBy { it.cliente?.regiao?.nome ?: "SEM_REGIAO" }
            .map { (regiao, lista) ->
                val valorTotal = lista.sumOf { it.total }
                VendaPorRegiaoItem(
                    regiao = regiao,
                    quantidade = lista.size,
                    valorTotal = valorTotal,
                    percentual = if (totalVendas > BigDecimal.ZERO)
                        (valorTotal.divide(totalVendas, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toDouble()
                    else 0.0
                )
            }
            .sortedByDescending { it.valorTotal }

        // Vendas por semana (ISO Week)
        val vendasPorSemana = vendas
            .groupBy {
                val date = it.dataVenda.toLocalDate()
                val weekYear = date.year
                val weekNum = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
                String.format("%04d-W%02d", weekYear, weekNum)
            }
            .map { (semana, lista) ->
                VendaPorPeriodoItem(
                    periodo = semana,
                    valorTotal = lista.sumOf { it.total },
                    quantidadeVendas = lista.size
                )
            }
            .sortedBy { it.periodo }

        // Vendas por dia
        val vendasPorDia = vendas
            .groupBy { it.dataVenda.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
            .map { (dia, lista) ->
                VendaPorPeriodoItem(
                    periodo = dia,
                    valorTotal = lista.sumOf { it.total },
                    quantidadeVendas = lista.size
                )
            }
            .sortedBy { it.periodo }

        // Top clientes
        val topClientes = vendas
            .filter { it.cliente != null }
            .groupBy { it.cliente!!.nome }
            .map { (clienteNome, lista) ->
                ClienteItem(
                    cliente = clienteNome,
                    totalCompras = lista.sumOf { it.total },
                    quantidadeCompras = lista.size
                )
            }
            .sortedByDescending { it.totalCompras }
            .take(10)

        return RelatorioVendasResponse(
            periodo = PeriodoRelatorio(inicio, fim),
            totalVendas = totalVendas,
            totalRecebido = totalRecebido,
            totalPendente = totalPendente,
            quantidadeVendas = quantidadeVendas,
            ticketMedio = ticketMedio,
            vendasPorSabor = vendasPorSabor,
            vendasPorRegiao = vendasPorRegiao,
            vendasPorSemana = vendasPorSemana,
            vendasPorDia = vendasPorDia,
            topClientes = topClientes
        )
    }

    // ==================== Relatório de Estoque ====================
    @LogCall
    fun gerarRelatorioEstoque(filialId: UUID): RelatorioEstoqueResponse {
        val materiaPrima = materiaPrimaRepository.findByFilialIdOrderByDataCriacaoDesc(filialId)
        val embalagens = embalagemRepository.findByFilialIdOrderByDataCriacaoDesc(filialId)
        val gelinhos = estoqueGelinhoRepository.findByFilialIdOrderByUltimaAtualizacaoDesc(filialId)

        val estoqueMateriaPrima = materiaPrima.map {
            val percentual = if (it.totalUnidades > 0)
                (it.estoqueUnidades.toDouble() / it.totalUnidades * 100) else 0.0
            val valorTotal = it.precoPorUnidade.multiply(BigDecimal(it.estoqueUnidades))

            EstoqueMateriaPrimaItem(
                nome = it.nome,
                sabor = it.sabor?.nome,
                estoqueUnidades = it.estoqueUnidades,
                totalUnidades = it.totalUnidades,
                percentualDisponivel = percentual,
                precoPorUnidade = it.precoPorUnidade,
                valorTotal = valorTotal
            )
        }

        val estoqueEmbalagem = embalagens.map {
            val percentual = if (it.totalUnidades > 0)
                (it.estoqueUnidades.toDouble() / it.totalUnidades * 100) else 0.0
            val valorTotal = it.precoPorUnidade.multiply(BigDecimal(it.estoqueUnidades))

            EstoqueEmbalagemItem(
                nome = it.nome,
                sabor = it.sabor?.nome,
                estoqueUnidades = it.estoqueUnidades,
                totalUnidades = it.totalUnidades,
                percentualDisponivel = percentual,
                precoPorUnidade = it.precoPorUnidade,
                valorTotal = valorTotal
            )
        }

        val estoqueGelinho = gelinhos.map {
            EstoqueGelinhoItem(
                sabor = it.sabor.nome,
                quantidade = it.quantidade
            )
        }

        val valorTotalEstoque = estoqueMateriaPrima.sumOf { it.valorTotal } +
                                estoqueEmbalagem.sumOf { it.valorTotal }

        // Alertas de estoque baixo (menos de 20%)
        val alertas = mutableListOf<AlertaEstoqueItem>()

        materiaPrima.forEach {
            val percentual = if (it.totalUnidades > 0)
                (it.estoqueUnidades.toDouble() / it.totalUnidades * 100) else 0.0
            if (percentual < 20 && percentual > 0) {
                alertas.add(AlertaEstoqueItem(
                    tipo = "MATERIA_PRIMA",
                    nome = it.nome,
                    sabor = it.sabor?.nome,
                    estoqueAtual = it.estoqueUnidades,
                    nivelCritico = if (percentual < 10) "CRITICO" else "BAIXO"
                ))
            }
        }

        embalagens.forEach {
            val percentual = if (it.totalUnidades > 0)
                (it.estoqueUnidades.toDouble() / it.totalUnidades * 100) else 0.0
            if (percentual < 20 && percentual > 0) {
                alertas.add(AlertaEstoqueItem(
                    tipo = "EMBALAGEM",
                    nome = it.nome,
                    sabor = it.sabor?.nome,
                    estoqueAtual = it.estoqueUnidades,
                    nivelCritico = if (percentual < 10) "CRITICO" else "BAIXO"
                ))
            }
        }

        return RelatorioEstoqueResponse(
            dataGeracao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            estoqueMateriaPrima = estoqueMateriaPrima,
            estoqueEmbalagem = estoqueEmbalagem,
            estoqueGelinho = estoqueGelinho,
            valorTotalEstoque = valorTotalEstoque,
            alertasEstoqueBaixo = alertas
        )
    }

    // ==================== Relatório de Produção ====================
    @LogCall
    fun gerarRelatorioProducao(inicio: String, fim: String, filialId: UUID): RelatorioProducaoResponse {
        val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
        val dataFim = LocalDateTime.parse("${fim}T23:59:59")

        val producoes = producaoRepository.findByFilialIdAndDataProducaoBetweenOrderByDataProducaoDesc(filialId, dataInicio, dataFim)

        val totalProducao = producoes.sumOf { it.quantidadeProduzida }

        // Produção por sabor
        val producaoPorSabor = producoes
            .groupBy { it.sabor.nome }
            .map { (sabor, lista) ->
                val quantidade = lista.sumOf { it.quantidadeProduzida }
                ProducaoPorSaborItem(
                    sabor = sabor,
                    quantidade = quantidade,
                    percentual = if (totalProducao > 0)
                        (quantidade.toDouble() / totalProducao * 100) else 0.0,
                    vezes = lista.size
                )
            }
            .sortedByDescending { it.quantidade }

        // Produção por dia
        val producaoPorDia = producoes
            .groupBy { it.dataProducao.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
            .map { (dia, lista) ->
                ProducaoPorPeriodoItem(
                    periodo = dia,
                    quantidade = lista.sumOf { it.quantidadeProduzida },
                    quantidadeProducoes = lista.size
                )
            }
            .sortedBy { it.periodo }

        val diasComProducao = producaoPorDia.size
        val mediaProducaoDiaria = if (diasComProducao > 0)
            totalProducao.toDouble() / diasComProducao else 0.0

        return RelatorioProducaoResponse(
            periodo = PeriodoRelatorio(inicio, fim),
            totalProducao = totalProducao,
            producaoPorSabor = producaoPorSabor,
            producaoPorDia = producaoPorDia,
            mediaProducaoDiaria = mediaProducaoDiaria,
            diasComProducao = diasComProducao
        )
    }

    // ==================== Relatório de Lucro ====================
    @LogCall
    fun gerarRelatorioLucro(inicio: String, fim: String, filialId: UUID): RelatorioLucroResponse {
        val dataInicio = LocalDateTime.parse("${inicio}T00:00:00")
        val dataFim = LocalDateTime.parse("${fim}T23:59:59")

        val vendas = vendaRepository.findByFilialIdAndDataVendaBetweenOrderByDataVendaDesc(filialId, dataInicio, dataFim)
        val despesas = despesaRepository.findByFilialIdAndDataCriacaoBetweenOrderByDataCriacaoDesc(filialId, dataInicio, dataFim)

        val totalVendas = vendas.sumOf { it.totalPago } // Considera apenas o que foi pago
        val totalDespesas = despesas.filter { it.dataPagamento != null }.sumOf { it.valor }
        val lucroLiquido = totalVendas - totalDespesas
        val margemLucro = if (totalVendas > BigDecimal.ZERO)
            (lucroLiquido.divide(totalVendas, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toDouble()
        else 0.0

        // Lucro por mês
        val vendasPorMes = vendas
            .groupBy { it.dataVenda.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
            .mapValues { (_, lista) -> lista.sumOf { it.totalPago } }

        val despesasPorMes = despesas
            .filter { it.dataPagamento != null }
            .groupBy { it.dataCriacao.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
            .mapValues { (_, lista) -> lista.fold(BigDecimal.ZERO) { acc, d -> acc + d.valor } }

        val meses = (vendasPorMes.keys + despesasPorMes.keys).toSet().sorted()

        val lucroPorMes = meses.map { mes ->
            val vendaMes = vendasPorMes[mes] ?: BigDecimal.ZERO
            val despesaMes = despesasPorMes[mes] ?: BigDecimal.ZERO
            val lucroMes = vendaMes - despesaMes
            val margemMes = if (vendaMes > BigDecimal.ZERO)
                (lucroMes.divide(vendaMes, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toDouble()
            else 0.0

            LucroPorPeriodoItem(
                periodo = mes,
                vendas = vendaMes,
                despesas = despesaMes,
                lucro = lucroMes,
                margemLucro = margemMes
            )
        }

        val receitaMedia = if (lucroPorMes.isNotEmpty())
            lucroPorMes.map { it.vendas }.reduce { acc, v -> acc + v }
                .divide(BigDecimal(lucroPorMes.size), 2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO

        val despesaMedia = if (lucroPorMes.isNotEmpty())
            lucroPorMes.map { it.despesas }.reduce { acc, v -> acc + v }
                .divide(BigDecimal(lucroPorMes.size), 2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO

        val lucroMedio = receitaMedia - despesaMedia

        val melhorMes = lucroPorMes.maxByOrNull { it.lucro }?.let {
            MelhorPiorMesItem(mes = it.periodo, valor = it.lucro)
        }

        val piorMes = lucroPorMes.minByOrNull { it.lucro }?.let {
            MelhorPiorMesItem(mes = it.periodo, valor = it.lucro)
        }

        return RelatorioLucroResponse(
            periodo = PeriodoRelatorio(inicio, fim),
            totalVendas = totalVendas,
            totalDespesas = totalDespesas,
            lucroLiquido = lucroLiquido,
            margemLucro = margemLucro,
            lucroPorMes = lucroPorMes,
            resumoFinanceiro = ResumoFinanceiro(
                receitaMedia = receitaMedia,
                despesaMedia = despesaMedia,
                lucroMedio = lucroMedio,
                melhorMes = melhorMes,
                piorMes = piorMes
            )
        )
    }
}
