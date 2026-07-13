package br.com.storehouse.api.controller

import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import br.com.storehouse.data.model.SolicitacaoInternaRequest
import br.com.storehouse.data.model.SolicitacaoInternaResponse
import br.com.storehouse.data.model.SolicitacaoInternaUpdateRequest
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.service.SolicitacaoInternaService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/solicitacoes-internas")
class SolicitacaoInternaController(private val service: SolicitacaoInternaService) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun criar(
        @RequestBody request: SolicitacaoInternaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<SolicitacaoInternaResponse> =
        ResponseEntity.ok(service.criar(usuario.filialId, usuario.email, request))

    @GetMapping
    fun listar(
        @RequestParam(required = false) status: StatusSolicitacaoInterna?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): List<SolicitacaoInternaResponse> =
        service.listar(usuario.filialId, status)

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun atualizar(
        @PathVariable id: String,
        @RequestBody request: SolicitacaoInternaUpdateRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<SolicitacaoInternaResponse> =
        ResponseEntity.ok(service.atualizar(usuario.filialId, id, request))
}
