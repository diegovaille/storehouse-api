package br.com.storehouse.api.controller

import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.model.VendaResponse
import br.com.storehouse.service.VendaService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vendas")
class VendaController(private val vendaService: VendaService) {

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
        return vendaService.listarVendasPorPeriodo(usuario.filialId, inicio, fim, apenasAtiva)
    }

    @DeleteMapping("/{id}")
    fun cancelarVenda(
        @PathVariable id: String,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<Void> {
        vendaService.cancelarVenda(usuario.filialId, id)
        return ResponseEntity.noContent().build()
    }

}



