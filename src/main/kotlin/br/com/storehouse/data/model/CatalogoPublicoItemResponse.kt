package br.com.storehouse.data.model

import br.com.storehouse.data.entities.Produto
import java.math.BigDecimal

/**
 * DTO de vitrine pública — usado pelo endpoint `GET /api/publico/catalogo/{filialId}`
 * (sem autenticação, ver issue #17).
 *
 * Deliberadamente estreito: só o que um visitante sem credencial pode ver. NUNCA adicionar
 * `precoCusto` (segredo comercial) nem o estoque numérico exato aqui — isso é dado de
 * operação, exposto só por `ProdutoResponse` (endpoint autenticado `GET /api/produtos`).
 * `disponivel` é a única informação de estoque que a vitrine expõe, e é derivada
 * (`estoque > 0`), nunca o número em si.
 */
data class CatalogoPublicoItemResponse(
    val nome: String,
    val imagemUrl: String?,
    val preco: BigDecimal,
    val disponivel: Boolean
)

/**
 * Produtos sem `estadoAtual` (nunca precificados) não entram na vitrine — não há preço de
 * venda para mostrar, e um produto sem preço não é vendável. `null` aqui sinaliza "não deve
 * aparecer no catálogo público"; o service filtra com `mapNotNull`.
 */
fun Produto.toCatalogoPublico(): CatalogoPublicoItemResponse? {
    val estado = this.estadoAtual ?: return null

    return CatalogoPublicoItemResponse(
        nome = this.nome,
        imagemUrl = this.imagemUrl,
        preco = estado.preco,
        disponivel = estado.estoque > 0
    )
}
