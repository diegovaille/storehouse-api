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
@RequestMapping("/api/vendas")
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



