package br.com.storehouse.data.model

import br.com.storehouse.data.entities.Produto
import java.math.BigDecimal

data class ProdutoResponse (
    val codigoBarras: String,
    val nome: String,
    val preco: BigDecimal,
    val precoCusto: BigDecimal,
    val estoque: Int,
    val tipo: String,
    val camposDescricao: Map<String, Any?> = emptyMap(),
    val imagemUrl: String? = null
)

fun Produto.toResponse(): ProdutoResponse {
    val campos = this.descricao?.descricaoCampos ?: emptyMap()

    return ProdutoResponse(
        codigoBarras = this.codigoBarras,
        nome = this.nome,
        preco = this.estadoAtual?.preco ?: BigDecimal.ZERO,
        estoque = this.estadoAtual?.estoque ?: 0,
        tipo = this.tipo.nome,
        camposDescricao = campos,
        imagemUrl = this.imagemUrl,
        precoCusto = this.estadoAtual?.precoCusto ?: BigDecimal.ZERO
    )
}