package br.com.storehouse.service

import br.com.storehouse.data.entities.InsightVendas
import br.com.storehouse.data.model.VendaResponse
import br.com.storehouse.data.repository.InsightVendasRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class InsightService(
    @Value("\${openai.api-key}") private val apiKey: String,
    private val vendaService: VendaService,
    private val insightRepo: InsightVendasRepository
) {
    private val client = WebClient.builder()
        .baseUrl("https://api.openai.com/v1/chat/completions")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .defaultHeader("Content-Type", "application/json")
        .build()

    fun gerarInsightDeVendas(
        filialId: UUID,
        inicio: String?,
        fim: String?,
        atualizarInsight: Boolean
    ): String {

        val dataInicio = inicio?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val dataFim = fim?.let { LocalDate.parse(it) } ?: LocalDate.now()

        // 🔹 Verifica se já existe insight para o mesmo período e filial
        val existente = insightRepo.findByFilialIdAndDataInicioAndDataFim(filialId, dataInicio, dataFim)

        if (existente != null && !atualizarInsight) {
            return existente.insight
        }

        val vendas = vendaService.listarVendasPorPeriodo(filialId, inicio, fim, true, relatorio = true)
        if (vendas.isEmpty())
            return "Não há vendas registradas nesse período."

        val resumo = gerarResumoDetalhado(vendas)

        val prompt = """
            Você é um analista de vendas de uma loja (focada em livros) de uma igreja batista.
            Analise o seguinte resumo de vendas e estoque, e gere:
            1. Um resumo sobre o desempenho de vendas e lucros.
            2. Itens com estoque baixo que precisam de recompra imediata.
            3. Sugestões de novos títulos com foco em: autoajuda cristã, devocionais, discipulado, casais e jovens.
            4. Recomendações práticas para aumentar as vendas da loja.

            Resumo do período:
            - Período: ${resumo.periodo}
            - Total de vendas: R$ ${resumo.totalVendas.setScale(2)}
            - Total de custo: R$ ${resumo.totalCustos.setScale(2)}
            - Lucro total: R$ ${resumo.lucroTotal.setScale(2)}
            - Itens mais vendidos: ${resumo.itensMaisVendidos.joinToString(", ")}
            - Itens com estoque crítico (≤ 1): ${if (resumo.itensCriticoEstoque.isEmpty()) "Nenhum" else resumo.itensCriticoEstoque.joinToString(", ")}

            Gere uma análise completa com base nesses dados.
        """.trimIndent()

        val payload = mapOf(
            "model" to "gpt-4o",
            "messages" to listOf(
                mapOf("role" to "user", "content" to prompt)
            )
        )

        val response = client.post()
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        val insight = (response?.get("choices") as? List<Map<String, Any>>)
            ?.firstOrNull()
            ?.get("message")
            ?.let { (it as Map<*, *>)["content"] as? String }
            ?: "Não foi possível gerar o insight."

        // 🔹 Atualiza ou cria novo insight
        val entity = existente?.copy(
            insight = insight,
            dataInicio = dataInicio,
            dataFim = dataFim
        ) ?: InsightVendas(
            filialId = filialId,
            dataInicio = dataInicio,
            dataFim = dataFim,
            insight = insight
        )

        insightRepo.save(entity)
        return insight
    }

    private fun gerarResumoDetalhado(vendas: List<VendaResponse>): ResumoFinanceiro {
        val totalVendas = vendas.sumOf { it.valorTotal }
        val totalCustos = vendas.flatMap { it.itens }
            .sumOf { it.precoCusto?.multiply(it.quantidade.toBigDecimal()) ?: BigDecimal.ZERO }

        val lucroTotal = totalVendas.subtract(totalCustos)

        val itensMaisVendidos = vendas
            .flatMap { it.itens }
            .groupBy { it.produtoNome }
            .mapValues { (_, itens) -> itens.sumOf { it.quantidade } }
            .toList()
            .sortedByDescending { it.second }
            .take(15)
            .map { it.first }

        val itensCriticoEstoque = vendas.flatMap { it.itens }
            .filter { (it.estoque ?: 0) <= 1 }
            .map { it.produtoNome }
            .distinct()

        val formatterInput = DateTimeFormatter.ISO_DATE_TIME
        val formatterOutput = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dataInicio = vendas.minOfOrNull { LocalDateTime.parse(it.data, formatterInput).toLocalDate() }?.format(formatterOutput) ?: "-"
        val dataFim = vendas.maxOfOrNull { LocalDateTime.parse(it.data, formatterInput).toLocalDate() }?.format(formatterOutput) ?: "-"

        return ResumoFinanceiro(
            totalVendas = totalVendas,
            totalCustos = totalCustos,
            lucroTotal = lucroTotal,
            itensMaisVendidos = itensMaisVendidos,
            itensCriticoEstoque = itensCriticoEstoque,
            periodo = "$dataInicio a $dataFim"
        )
    }

    data class ResumoFinanceiro(
        val totalVendas: BigDecimal,
        val totalCustos: BigDecimal,
        val lucroTotal: BigDecimal,
        val itensMaisVendidos: List<String>,
        val itensCriticoEstoque: List<String>,
        val periodo: String
    )
}
