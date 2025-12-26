package br.com.pinguimice.admin.controller

import br.com.pinguimice.admin.model.DespesaRequest
import br.com.pinguimice.admin.model.DespesaResponse
import br.com.pinguimice.admin.service.DespesaService
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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

    // JSON simples (sem upload)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun criarDespesaJson(
        @RequestBody request: DespesaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): DespesaResponse {
        return despesaService.criarDespesa(request, usuario.filialId)
    }

    // Multipart (com upload opcional)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun criarDespesaMultipart(
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

    // JSON simples (sem upload)
    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun atualizarDespesaJson(
        @PathVariable id: UUID,
        @RequestBody request: DespesaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): DespesaResponse {
        return despesaService.atualizarDespesa(id, request, usuario.filialId)
    }

    // Multipart (com upload opcional)
    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun atualizarDespesaMultipart(
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
