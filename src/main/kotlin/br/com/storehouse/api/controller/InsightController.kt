package br.com.storehouse.api.controller

import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.service.InsightService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/insights")
class InsightController(
    private val insightService: InsightService
) {

    @GetMapping("/vendas")
    fun gerarInsightVendas(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @RequestParam(defaultValue = "false") atualizarInsight: Boolean = false,
    ): Map<String, String> {
        val insight = insightService.gerarInsightDeVendas(usuario.filialId, inicio, fim, atualizarInsight)
        return mapOf("insight" to insight)
    }

}