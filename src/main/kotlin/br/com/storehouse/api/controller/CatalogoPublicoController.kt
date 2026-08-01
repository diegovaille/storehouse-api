package br.com.storehouse.api.controller

import br.com.storehouse.data.model.CatalogoPublicoItemResponse
import br.com.storehouse.service.ProdutoService
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Vitrine pública (issue #17) — membros da igreja veem o catálogo de uma filial sem
 * credencial. Deliberadamente SEM `@AuthenticationPrincipal` e SEM `@PreAuthorize`: é a
 * única rota do sistema pensada para ser anônima. A filial vem explícita na URL (não há JWT
 * de onde tirá-la). `SecurityConfig` libera todas as rotas GET sob `/api/publico`.
 *
 * Não confundir com `GET /api/produtos` (autenticado, endpoint de operação da loja — preço
 * de custo, estoque exato). Este endpoint usa `CatalogoPublicoItemResponse`, um DTO próprio
 * que nunca expõe esses dois campos.
 */
@RestController
@RequestMapping("/api/publico/catalogo")
class CatalogoPublicoController(
    private val produtoService: ProdutoService
) {

    @GetMapping("/{filialId}")
    fun listar(@PathVariable filialId: UUID): ResponseEntity<List<CatalogoPublicoItemResponse>> {
        val itens = produtoService.listarVitrine(filialId)

        // Única rota anônima do sistema, exposta à internet sem rate limiting (fora de
        // escopo aqui — ver PR). Um Cache-Control curto reduz a chance de um scraper simples
        // martelar o banco a cada request; não substitui um rate limiter de verdade.
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
            .body(itens)
    }
}
