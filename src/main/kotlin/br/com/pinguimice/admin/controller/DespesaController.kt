package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.DespesaRequest
import br.com.pinguimice.admin.model.DespesaResponse
import br.com.pinguimice.admin.service.DespesaService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/pinguim-admin/despesas")
class DespesaController(
    private val despesaService: DespesaService
) {

    @GetMapping
    fun listarDespesas(
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): List<DespesaResponse> {
        return despesaService.listarDespesas(inicio, fim, usuario.filialId)
    }

    @PostMapping(consumes = ["multipart/form-data"])
    @ResponseStatus(HttpStatus.CREATED)
    fun criarDespesa(
        @RequestPart("dados") request: DespesaRequest,
        @RequestPart("arquivo", required = false) arquivo: MultipartFile?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): DespesaResponse {
        return despesaService.criarDespesa(
            request,
            usuario.filialId,
            arquivo?.bytes,
            arquivo?.originalFilename,
            arquivo?.contentType
        )
    }

    @PutMapping("/{id}", consumes = ["multipart/form-data"])
    fun atualizarDespesa(
        @PathVariable id: UUID,
        @RequestPart("dados") request: DespesaRequest,
        @RequestPart("arquivo", required = false) arquivo: MultipartFile?,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): DespesaResponse {
        return despesaService.atualizarDespesa(
            id,
            request,
            usuario.filialId,
            arquivo?.bytes,
            arquivo?.originalFilename,
            arquivo?.contentType
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletarDespesa(
        @PathVariable id: UUID,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ) {
        despesaService.deletarDespesa(id, usuario.filialId)
    }
}
