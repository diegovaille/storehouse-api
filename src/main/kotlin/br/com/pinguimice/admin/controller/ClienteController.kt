package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.ClienteRequest
import br.com.pinguimice.admin.model.ClienteResponse
import br.com.pinguimice.admin.service.ClienteService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/pinguim-admin/clientes")
class ClienteController(
    private val clienteService: ClienteService
) {

    @GetMapping
    fun listarClientes(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @RequestParam(defaultValue = "false") apenasAtivos: Boolean
    ): List<ClienteResponse> {
        return clienteService.listarClientes(usuario.filialId, apenasAtivos)
    }

    @GetMapping("/{id}")
    fun buscarPorId(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.buscarPorId(id, usuario.filialId)
        return ResponseEntity.ok(cliente)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criarCliente(
        @RequestBody request: ClienteRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ClienteResponse {
        return clienteService.criarCliente(request, usuario.filialId)
    }

    @PutMapping("/{id}")
    fun atualizarCliente(
        @PathVariable id: UUID,
        @RequestBody request: ClienteRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.atualizarCliente(id, request, usuario.filialId)
        return ResponseEntity.ok(cliente)
    }

    @PatchMapping("/{id}/bloquear")
    fun bloquearCliente(
        @PathVariable id: UUID,
        @RequestBody request: Map<String, String>,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<ClienteResponse> {
        val motivo = request["motivo"] ?: "Não especificado"
        val cliente = clienteService.bloquearCliente(id, motivo, usuario.filialId)
        return ResponseEntity.ok(cliente)
    }

    @PatchMapping("/{id}/desbloquear")
    fun desbloquearCliente(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.desbloquearCliente(id, usuario.filialId)
        return ResponseEntity.ok(cliente)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarCliente(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) {
        clienteService.deletarCliente(id, usuario.filialId)
    }
}

