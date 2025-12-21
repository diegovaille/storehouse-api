package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.service.PinguimRelatorioService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pinguimice-admin/relatorios")
class PinguimRelatorioController(
    private val pinguimRelatorioService: PinguimRelatorioService
) {

    @GetMapping("/despesas")
    fun gerarRelatorioDespesas(
        @RequestParam inicio: String,
        @RequestParam fim: String
    ): RelatorioDespesasResponse {
        return pinguimRelatorioService.gerarRelatorioDespesas(inicio, fim)
    }

    @GetMapping("/vendas")
    fun gerarRelatorioVendas(
        @RequestParam inicio: String,
        @RequestParam fim: String
    ): RelatorioVendasResponse {
        return pinguimRelatorioService.gerarRelatorioVendas(inicio, fim)
    }

    @GetMapping("/estoque")
    fun gerarRelatorioEstoque(): RelatorioEstoqueResponse {
        return pinguimRelatorioService.gerarRelatorioEstoque()
    }

    @GetMapping("/producao")
    fun gerarRelatorioProducao(
        @RequestParam inicio: String,
        @RequestParam fim: String
    ): RelatorioProducaoResponse {
        return pinguimRelatorioService.gerarRelatorioProducao(inicio, fim)
    }

    @GetMapping("/lucro")
    fun gerarRelatorioLucro(
        @RequestParam inicio: String,
        @RequestParam fim: String
    ): RelatorioLucroResponse {
        return pinguimRelatorioService.gerarRelatorioLucro(inicio, fim)
    }
}

