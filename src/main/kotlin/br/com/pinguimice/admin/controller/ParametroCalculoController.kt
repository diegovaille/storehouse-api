package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.ParametroCalculoRequest
import br.com.pinguimice.admin.model.ParametroCalculoResponse
import br.com.pinguimice.admin.service.ParametroCalculoService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pinguim-admin/parametros-calculo")
class ParametroCalculoController(
    private val parametroService: ParametroCalculoService
) {

    @GetMapping
    fun listarParametros(): List<ParametroCalculoResponse> {
        return parametroService.listarParametros()
    }

    @GetMapping("/{chave}")
    fun buscarPorChave(@PathVariable chave: String): ParametroCalculoResponse {
        return parametroService.buscarPorChave(chave)
    }

    @PutMapping("/{chave}")
    fun atualizarParametro(
        @PathVariable chave: String,
        @RequestBody request: ParametroCalculoRequest
    ): ParametroCalculoResponse {
        return parametroService.atualizarParametro(chave, request)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criarParametro(
        @RequestBody request: ParametroCalculoRequest
    ): ParametroCalculoResponse {
        return parametroService.criarParametro(request)
    }
}

