package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.SaborRequest
import br.com.pinguimice.admin.model.SaborResponse
import br.com.pinguimice.admin.service.SaborService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pinguimice-admin/sabores")
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
        @RequestBody request: SaborRequest
    ): SaborResponse {
        return saborService.criarSabor(request)
    }
}
