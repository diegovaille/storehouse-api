package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.SaborRequest
import br.com.pinguimice.admin.model.SaborResponse
import br.com.pinguimice.admin.service.SaborService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/pinguim-admin/sabores")
class SaborController(
    private val saborService: SaborService
) {

    @GetMapping
    fun listarSabores(
        @RequestParam(defaultValue = "true") apenasAtivos: Boolean
    ): List<SaborResponse> {
        return saborService.listarSabores(apenasAtivos)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criarSabor(
        @RequestBody request: SaborRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): SaborResponse {
        return saborService.criarSabor(request, usuario.filialId)
    }

    @PutMapping("/{id}")
    fun editarSabor(
        @PathVariable id: UUID,
        @RequestBody request: SaborRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): SaborResponse {
        return saborService.editarSabor(id, request, usuario.filialId)
    }
}
