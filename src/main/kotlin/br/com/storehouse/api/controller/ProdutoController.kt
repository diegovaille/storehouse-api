package br.com.storehouse.api.controller

import br.com.storehouse.data.model.ProdutoDto
import br.com.storehouse.data.model.ProdutoResponse
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.model.toResponse
import br.com.storehouse.service.IsbnLookupService
import br.com.storehouse.service.ProdutoService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/produtos")
class ProdutoController(
    private val isbnService: IsbnLookupService,
    private val produtoService: ProdutoService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun cadastrarOuAtualizarComImagem(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @RequestPart("dados") dados: ProdutoDto,
        @RequestPart("imagem", required = false) imagem: MultipartFile?
    ): ResponseEntity<ProdutoResponse> {
        val produto = produtoService.cadastrarOuAtualizar(usuario.filialId, dados, imagem?.bytes)
        return ResponseEntity.ok(produto.toResponse())
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun cadastrarOuAtualizarSemImagem(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @RequestBody dados: ProdutoDto
    ): ResponseEntity<ProdutoResponse> {
        val produto = produtoService.cadastrarOuAtualizar(usuario.filialId, dados, null)
        return ResponseEntity.ok(produto.toResponse())
    }

    @GetMapping
    fun listar(
        @AuthenticationPrincipal usuario: UsuarioAutenticado
    ): List<ProdutoResponse> =
        produtoService.listarTodos(usuario.filialId).map { it.toResponse() }

    @GetMapping("/{codigo}")
    fun buscarPorCodigo(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable codigo: String
    ): ResponseEntity<ProdutoResponse> =
        produtoService.buscarPorCodigo(usuario.filialId, codigo)
            ?.let { ResponseEntity.ok(it.toResponse()) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/{codigo}")
    fun remover(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable codigo: String
    ): ResponseEntity<Void> {
        produtoService.removerLogicamente(usuario.filialId, codigo)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{codigo}/estoque")
    fun atualizarEstoque(
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
        @PathVariable codigo: String,
        @RequestParam novoEstoque: Int
    ): ResponseEntity<Void> {
        produtoService.atualizarEstoque(usuario.filialId, codigo, novoEstoque)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/isbn/{codigo}")
    fun buscarPorIsbn(@PathVariable codigo: String): ResponseEntity<Map<String, Any>> {
        val info = isbnService.buscarInformacoesPorIsbn(codigo)
        return ResponseEntity.ok(info)
    }
}
