package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.ProducaoRequest
import br.com.pinguimice.admin.model.ProducaoResponse
import br.com.pinguimice.admin.service.ProducaoService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/pinguim-admin/producao")
class ProducaoController(
    private val producaoService: ProducaoService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrarProducao(
        @RequestBody request: ProducaoRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): ProducaoResponse {
        return producaoService.registrarProducao(request, usuario.filialId)
    }

    @GetMapping
    fun listarProducao(
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): List<ProducaoResponse> {
        return producaoService.listarProducao(inicio, fim, usuario.filialId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluirProducao(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) {
        producaoService.excluirProducao(id, usuario.filialId)
    }
}
