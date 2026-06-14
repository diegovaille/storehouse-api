package br.com.storehouse.api.controller

import br.com.storehouse.data.enums.StatusSolicitacao
import br.com.storehouse.data.model.SolicitacaoRequest
import br.com.storehouse.data.model.SolicitacaoResponse
import br.com.storehouse.data.model.SolicitacaoUpdateRequest
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.service.SolicitacaoService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/solicitacoes")
class SolicitacaoController(private val service: SolicitacaoService) {

    @PostMapping
    fun criar(
        @RequestBody request: SolicitacaoRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<SolicitacaoResponse> =
        ResponseEntity.ok(service.criar(usuario.filialId, request))

    @GetMapping
    fun listar(
        @RequestParam(required = false) status: StatusSolicitacao?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): List<SolicitacaoResponse> =
        service.listar(usuario.filialId, status)

    @PatchMapping("/{id}")
    fun atualizar(
        @PathVariable id: String,
        @RequestBody request: SolicitacaoUpdateRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<SolicitacaoResponse> =
        ResponseEntity.ok(service.atualizar(usuario.filialId, id, request))
}
