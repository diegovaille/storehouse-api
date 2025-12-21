package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.PinguimVendaRequest
import br.com.pinguimice.admin.model.PinguimVendaResponse
import br.com.pinguimice.admin.service.PinguimVendaService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/pinguimice-admin/vendas")
class PinguimVendaController(
    private val vendaService: PinguimVendaService
) {

    @PostMapping
    fun registrarVenda(
        @RequestBody request: PinguimVendaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<PinguimVendaResponse> {
        val venda = vendaService.registrarVenda(request, usuario.email)
        return ResponseEntity.status(HttpStatus.CREATED).body(venda)
    }

    @GetMapping
    fun listarVendas(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) inicio: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fim: LocalDateTime?
    ): List<PinguimVendaResponse> {
        return vendaService.listarVendas(inicio, fim)
    }

    @PutMapping("/{id}")
    fun editarVenda(
        @PathVariable id: UUID,
        @RequestBody request: PinguimVendaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<PinguimVendaResponse> {
        val venda = vendaService.editarVenda(id, request, usuario.email)
        return ResponseEntity.ok(venda)
    }

    @PatchMapping("/{id}/pagar")
    fun marcarComoPaga(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<PinguimVendaResponse> {
        val venda = vendaService.marcarComoPaga(id, usuario.email)
        return ResponseEntity.ok(venda)
    }

    @DeleteMapping("/{id}")
    fun cancelarVenda(
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        vendaService.cancelarVenda(id)
        return ResponseEntity.noContent().build()
    }
}
