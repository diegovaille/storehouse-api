package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.service.PinguimRelatorioService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pinguim-admin/relatorios")
class PinguimRelatorioController(
    private val pinguimRelatorioService: PinguimRelatorioService
) {

    @GetMapping("/despesas")
    fun gerarRelatorioDespesas(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): RelatorioDespesasResponse {
        return pinguimRelatorioService.gerarRelatorioDespesas(inicio, fim, usuario.filialId)
    }

    @GetMapping("/vendas")
    fun gerarRelatorioVendas(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): RelatorioVendasResponse {
        return pinguimRelatorioService.gerarRelatorioVendas(inicio, fim, usuario.filialId)
    }

    @GetMapping("/estoque")
    fun gerarRelatorioEstoque(
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): RelatorioEstoqueResponse {
        return pinguimRelatorioService.gerarRelatorioEstoque(usuario.filialId)
    }

    @GetMapping("/producao")
    fun gerarRelatorioProducao(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): RelatorioProducaoResponse {
        return pinguimRelatorioService.gerarRelatorioProducao(inicio, fim, usuario.filialId)
    }

    @GetMapping("/lucro")
    fun gerarRelatorioLucro(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): RelatorioLucroResponse {
        return pinguimRelatorioService.gerarRelatorioLucro(inicio, fim, usuario.filialId)
    }
}
