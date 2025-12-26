package br.com.pinguimice.admin.model

import java.math.BigDecimal

// ==================== Relatório de Despesas ====================
data class RelatorioDespesasResponse(
    val periodo: PeriodoRelatorio,
    val totalDespesas: BigDecimal,
    val totalPago: BigDecimal,
    val totalPendente: BigDecimal,
    val despesasPorCategoria: List<DespesaPorCategoriaItem>,
    val despesasPorSemana: List<DespesaPorPeriodoItem>,
    val despesasPorMes: List<DespesaPorPeriodoItem>,
    val despesasDetalhadas: List<DespesaDetalhadaItem>
)

data class DespesaPorCategoriaItem(
    val categoria: String,
    val valor: BigDecimal,
    val percentual: Double,
    val quantidade: Int
)

data class DespesaPorPeriodoItem(
    val periodo: String, // YYYY-MM ou YYYY-MM-DD
    val valor: BigDecimal,
    val quantidade: Int
)

data class DespesaDetalhadaItem(
    val descricao: String,
    val valor: BigDecimal,
    val dataPagamento: String?,
    val dataVencimento: String?,
    val status: String // PAGO, PENDENTE, VENCIDO
)

// ==================== Relatório de Vendas ====================
data class RelatorioVendasResponse(
    val periodo: PeriodoRelatorio,
    val totalVendas: BigDecimal,
    val totalRecebido: BigDecimal,
    val totalPendente: BigDecimal,
    val quantidadeVendas: Int,
    val ticketMedio: BigDecimal,
    val vendasPorSabor: List<VendaPorSaborItem>,
    val vendasPorRegiao: List<VendaPorRegiaoItem>,
    val vendasPorSemana: List<VendaPorPeriodoItem>,
    val vendasPorDia: List<VendaPorPeriodoItem>,
    val topClientes: List<ClienteItem>
)

data class VendaPorSaborItem(
    val sabor: String,
    val quantidade: Int,
    val valorTotal: BigDecimal,
    val percentualQuantidade: Double,
    val percentualValor: Double
)

data class VendaPorRegiaoItem(
    val regiao: String,
    val quantidade: Int,
    val valorTotal: BigDecimal,
    val percentual: Double
)

data class VendaPorPeriodoItem(
    val periodo: String, // YYYY-MM-DD
    val valorTotal: BigDecimal,
    val quantidadeVendas: Int
)

data class ClienteItem(
    val cliente: String,
    val totalCompras: BigDecimal,
    val quantidadeCompras: Int
)

// ==================== Relatório de Estoque ====================
data class RelatorioEstoqueResponse(
    val dataGeracao: String,
    val estoqueMateriaPrima: List<EstoqueMateriaPrimaItem>,
    val estoqueEmbalagem: List<EstoqueEmbalagemItem>,
    val estoqueGelinho: List<EstoqueGelinhoItem>,
    val valorTotalEstoque: BigDecimal,
    val alertasEstoqueBaixo: List<AlertaEstoqueItem>
)

data class EstoqueMateriaPrimaItem(
    val nome: String,
    val sabor: String?,
    val estoqueUnidades: Int,
    val totalUnidades: Int,
    val percentualDisponivel: Double,
    val precoPorUnidade: BigDecimal,
    val valorTotal: BigDecimal
)

data class EstoqueEmbalagemItem(
    val nome: String,
    val sabor: String?,
    val estoqueUnidades: Int,
    val totalUnidades: Int,
    val percentualDisponivel: Double,
    val precoPorUnidade: BigDecimal,
    val valorTotal: BigDecimal
)

data class EstoqueGelinhoItem(
    val sabor: String,
    val quantidade: Int
)

data class AlertaEstoqueItem(
    val tipo: String, // MATERIA_PRIMA, EMBALAGEM
    val nome: String,
    val sabor: String?,
    val estoqueAtual: Int,
    val nivelCritico: String // CRITICO, BAIXO
)

// ==================== Relatório de Produção ====================
data class RelatorioProducaoResponse(
    val periodo: PeriodoRelatorio,
    val totalProducao: Int,
    val producaoPorSabor: List<ProducaoPorSaborItem>,
    val producaoPorDia: List<ProducaoPorPeriodoItem>,
    val mediaProducaoDiaria: Double,
    val diasComProducao: Int
)

data class ProducaoPorSaborItem(
    val sabor: String,
    val quantidade: Int,
    val percentual: Double,
    val vezes: Int // número de produções
)

data class ProducaoPorPeriodoItem(
    val periodo: String, // YYYY-MM-DD
    val quantidade: Int,
    val quantidadeProducoes: Int
)

// ==================== Relatório de Lucro ====================
data class RelatorioLucroResponse(
    val periodo: PeriodoRelatorio,
    val totalVendas: BigDecimal,
    val totalDespesas: BigDecimal,
    val lucroLiquido: BigDecimal,
    val margemLucro: Double, // percentual
    val lucroPorMes: List<LucroPorPeriodoItem>,
    val resumoFinanceiro: ResumoFinanceiro
)

data class LucroPorPeriodoItem(
    val periodo: String, // YYYY-MM
    val vendas: BigDecimal,
    val despesas: BigDecimal,
    val lucro: BigDecimal,
    val margemLucro: Double
)

data class ResumoFinanceiro(
    val receitaMedia: BigDecimal,
    val despesaMedia: BigDecimal,
    val lucroMedio: BigDecimal,
    val melhorMes: MelhorPiorMesItem?,
    val piorMes: MelhorPiorMesItem?
)

data class MelhorPiorMesItem(
    val mes: String,
    val valor: BigDecimal
)

// ==================== Comum ====================
data class PeriodoRelatorio(
    val inicio: String,
    val fim: String
)

