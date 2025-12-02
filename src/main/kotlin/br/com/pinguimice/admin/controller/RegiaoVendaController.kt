package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.RegiaoVendaRequest
import br.com.pinguimice.admin.model.RegiaoVendaResponse
import br.com.pinguimice.admin.service.RegiaoVendaService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/pinguimice-admin/regioes")
class RegiaoVendaController(
    private val regiaoVendaService: RegiaoVendaService
) {

    @GetMapping
    fun listarRegioes(
        @RequestParam(defaultValue = "true") apenasAtivos: Boolean
    ): List<RegiaoVendaResponse> {
        return regiaoVendaService.listarRegioes(apenasAtivos)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criarRegiao(
        @RequestBody request: RegiaoVendaRequest
    ): RegiaoVendaResponse {
        return regiaoVendaService.criarRegiao(request)
    }

    @PutMapping("/{id}")
    fun atualizarRegiao(
        @PathVariable id: UUID,
        @RequestBody request: RegiaoVendaRequest
    ): RegiaoVendaResponse {
        return regiaoVendaService.atualizarRegiao(id, request)
    }
}
