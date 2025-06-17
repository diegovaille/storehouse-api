package br.com.storehouse.data.model

import java.math.BigDecimal
import java.util.*

// ProdutoDto.kt
data class ProdutoDto(
    val codigoBarras: String,
    val tipoId: UUID,
    val nome: String,
    val preco: BigDecimal,
    val precoCusto: BigDecimal,
    val estoque: Int,
    val descricaoCampos: Map<String, Any>? = null, // ← Novo campo
    val imagemUrl: String? = null // Referencia para imagem remota
)