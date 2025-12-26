package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.service.EstoqueService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/pinguim-admin")
class EstoqueController(
    private val estoqueService: EstoqueService
) {

    // ==================== MATÉRIA PRIMA (INSUMOS) ====================

    @GetMapping("/materia-prima")
    fun listarMateriaPrima(@AuthenticationPrincipal usuario: UsuarioAutenticado): List<MateriaPrimaResponse> {
        return estoqueService.listarMateriaPrima(usuario.filialId)
    }

    @PostMapping("/materia-prima")
    @ResponseStatus(HttpStatus.CREATED)
    fun criarMateriaPrima(
        @RequestBody request: MateriaPrimaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): MateriaPrimaResponse {
        return estoqueService.criarMateriaPrima(request, usuario.filialId)
    }

    @PutMapping("/materia-prima/{id}")
    fun atualizarMateriaPrima(
        @PathVariable id: UUID,
        @RequestBody request: MateriaPrimaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): MateriaPrimaResponse {
        return estoqueService.atualizarMateriaPrima(id, request, usuario.filialId)
    }

    @DeleteMapping("/materia-prima/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarMateriaPrima(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) {
        estoqueService.deletarMateriaPrima(id, usuario.filialId)
    }

    // ==================== EMBALAGEM ====================

    @GetMapping("/embalagem")
    fun listarEmbalagens(@AuthenticationPrincipal usuario: UsuarioAutenticado): List<EmbalagemResponse> {
        return estoqueService.listarEmbalagens(usuario.filialId)
    }

    @PostMapping("/embalagem")
    @ResponseStatus(HttpStatus.CREATED)
    fun criarEmbalagem(
        @RequestBody request: EmbalagemRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): EmbalagemResponse {
        return estoqueService.criarEmbalagem(request, usuario.filialId)
    }

    @PutMapping("/embalagem/{id}")
    fun atualizarEmbalagem(
        @PathVariable id: UUID,
        @RequestBody request: EmbalagemRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): EmbalagemResponse {
        return estoqueService.atualizarEmbalagem(id, request, usuario.filialId)
    }

    @DeleteMapping("/embalagem/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarEmbalagem(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) {
        estoqueService.deletarEmbalagem(id, usuario.filialId)
    }

    // ==================== OUTROS ====================

    @GetMapping("/outros")
    fun listarOutros(@AuthenticationPrincipal usuario: UsuarioAutenticado): List<OutrosResponse> {
        return estoqueService.listarOutros(usuario.filialId)
    }

    @PostMapping("/outros")
    @ResponseStatus(HttpStatus.CREATED)
    fun criarOutros(
        @RequestBody request: OutrosRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): OutrosResponse {
        return estoqueService.criarOutros(request, usuario.filialId)
    }

    @PutMapping("/outros/{id}")
    fun atualizarOutros(
        @PathVariable id: UUID,
        @RequestBody request: OutrosRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): OutrosResponse {
        return estoqueService.atualizarOutros(id, request, usuario.filialId)
    }

    @DeleteMapping("/outros/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarOutros(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) {
        estoqueService.deletarOutros(id, usuario.filialId)
    }

    // ==================== ESTOQUE GELINHO ====================

    @GetMapping("/estoque-gelinho")
    fun listarEstoqueGelinho(@AuthenticationPrincipal usuario: UsuarioAutenticado): List<EstoqueGelinhoResponse> {
        return estoqueService.listarEstoqueGelinho(usuario.filialId)
    }
}
