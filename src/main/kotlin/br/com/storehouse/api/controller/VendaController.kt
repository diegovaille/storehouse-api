package br.com.storehouse.api.controller

import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.model.VendaResponse
import br.com.storehouse.service.RelatorioService
import br.com.storehouse.service.VendaService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(value = ["/api/vendas", "/api/v1/vendas"])
class VendaController(private val vendaService: VendaService, val relatorioService: RelatorioService) {

    @PostMapping
    fun realizarVenda(
        @RequestBody request: VendaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
    ): ResponseEntity<VendaResponse> {
        val venda = vendaService.registrarVenda(
            filialId = usuario.filialId,
            request = request,
            emailUsuario = usuario.email
        )
        return ResponseEntity.ok(venda)
    }

    @GetMapping
    fun listarPorPeriodo(
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @RequestParam(required = false) apenasAtiva: Boolean = true,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): List<VendaResponse> {
        return vendaService.listarVendasPorPeriodo(usuario.filialId, inicio, fim, apenasAtiva, false)
    }

    @GetMapping("/resumo")
    fun resumo(
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) = vendaService.resumoVendas(usuario.filialId, inicio, fim)

    @GetMapping("/recentes")
    fun recentes(
        @RequestParam(defaultValue = "4") limite: Int,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) = vendaService.vendasRecentes(usuario.filialId, limite)

    @GetMapping("/mais-vendidos")
    fun maisVendidos(
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @RequestParam(defaultValue = "5") limite: Int,
        @RequestParam(required = false) categoria: String?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) = vendaService.maisVendidos(usuario.filialId, inicio, fim, limite, categoria)

    @GetMapping("/serie")
    fun serie(
        @RequestParam(defaultValue = "7") dias: Int,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) = vendaService.serieVendas(usuario.filialId, dias)

    @DeleteMapping("/{id}")
    fun cancelarVenda(
        @PathVariable id: String,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<Void> {
        vendaService.cancelarVenda(usuario.filialId, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/relatorio/pdf")
    fun gerarRelatorioPdf(
        @RequestParam inicio: String?,
        @RequestParam fim: String?,
        @RequestParam(defaultValue = "true") apenasAtivas: Boolean,
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        response: HttpServletResponse
    ) {
        val vendas = vendaService.listarVendasPorPeriodo(usuario.filialId, inicio, fim, apenasAtivas, true)

        val pdf = relatorioService.gerarPdf(vendas)

        response.contentType = "application/pdf"
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-vendas.pdf")
        response.outputStream.use { it.write(pdf) }
    }

}



