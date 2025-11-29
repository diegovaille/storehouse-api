package br.com.storehouse.api.controller

import br.com.storehouse.data.model.NovoUsuarioRequest
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.model.UsuarioResponse
import br.com.storehouse.service.AdminUserService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/usuarios")
class AdminUsuarioController(
    private val adminUserService: AdminUserService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    fun create(
        @RequestBody request: NovoUsuarioRequest,
        @AuthenticationPrincipal principal: UsuarioAutenticado
    ): UsuarioResponse {
        return adminUserService.createUserForSameOrganization(request, principal.organizacaoId)
    }
}
