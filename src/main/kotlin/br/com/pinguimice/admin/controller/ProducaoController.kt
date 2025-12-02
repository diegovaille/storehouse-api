package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.ProducaoRequest
import br.com.pinguimice.admin.model.ProducaoResponse
import br.com.pinguimice.admin.service.ProducaoService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pinguimice-admin/producao")
class ProducaoController(
    private val producaoService: ProducaoService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrarProducao(
        @RequestBody request: ProducaoRequest
    ): ProducaoResponse {
        return producaoService.registrarProducao(request)
    }

    @GetMapping
    fun listarProducao(
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?
    ): List<ProducaoResponse> {
        return producaoService.listarProducao(inicio, fim)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluirProducao(@PathVariable id: java.util.UUID) {
        producaoService.excluirProducao(id)
    }
}
