package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.service.EstoqueService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/pinguimice-admin")
class EstoqueController(
    private val estoqueService: EstoqueService
) {

    // ==================== MATÉRIA PRIMA (INSUMOS) ====================

    @GetMapping("/materia-prima")
    fun listarMateriaPrima(): List<MateriaPrimaResponse> {
        return estoqueService.listarMateriaPrima()
    }

    @PostMapping("/materia-prima")
    @ResponseStatus(HttpStatus.CREATED)
    fun criarMateriaPrima(
        @RequestBody request: MateriaPrimaRequest
    ): MateriaPrimaResponse {
        return estoqueService.criarMateriaPrima(request)
    }

    @PutMapping("/materia-prima/{id}")
    fun atualizarMateriaPrima(
        @PathVariable id: UUID,
        @RequestBody request: MateriaPrimaRequest
    ): MateriaPrimaResponse {
        return estoqueService.atualizarMateriaPrima(id, request)
    }

    @DeleteMapping("/materia-prima/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarMateriaPrima(
        @PathVariable id: UUID
    ) {
        estoqueService.deletarMateriaPrima(id)
    }

    // ==================== EMBALAGEM ====================

    @GetMapping("/embalagem")
    fun listarEmbalagens(): List<EmbalagemResponse> {
        return estoqueService.listarEmbalagens()
    }

    @PostMapping("/embalagem")
    @ResponseStatus(HttpStatus.CREATED)
    fun criarEmbalagem(
        @RequestBody request: EmbalagemRequest
    ): EmbalagemResponse {
        return estoqueService.criarEmbalagem(request)
    }

    @PutMapping("/embalagem/{id}")
    fun atualizarEmbalagem(
        @PathVariable id: UUID,
        @RequestBody request: EmbalagemRequest
    ): EmbalagemResponse {
        return estoqueService.atualizarEmbalagem(id, request)
    }

    @DeleteMapping("/embalagem/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarEmbalagem(
        @PathVariable id: UUID
    ) {
        estoqueService.deletarEmbalagem(id)
    }

    // ==================== OUTROS ====================

    @GetMapping("/outros")
    fun listarOutros(): List<OutrosResponse> {
        return estoqueService.listarOutros()
    }

    @PostMapping("/outros")
    @ResponseStatus(HttpStatus.CREATED)
    fun criarOutros(
        @RequestBody request: OutrosRequest
    ): OutrosResponse {
        return estoqueService.criarOutros(request)
    }

    @PutMapping("/outros/{id}")
    fun atualizarOutros(
        @PathVariable id: UUID,
        @RequestBody request: OutrosRequest
    ): OutrosResponse {
        return estoqueService.atualizarOutros(id, request)
    }

    @DeleteMapping("/outros/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarOutros(
        @PathVariable id: UUID
    ) {
        estoqueService.deletarOutros(id)
    }
    // ==================== ESTOQUE GELINHO ====================

    @GetMapping("/estoque-gelinho")
    fun listarEstoqueGelinho(): List<EstoqueGelinhoResponse> {
        return estoqueService.listarEstoqueGelinho()
    }
}
