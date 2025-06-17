package br.com.storehouse.data.model

import java.util.*

// ProdutoDto.kt
data class ProdutoDto(
    val codigoBarras: String,
    val tipoId: UUID,
    val nome: String,
    val preco: Double,
    val precoCusto: Double,
    val estoque: Int,
    val descricaoCampos: Map<String, Any>? = null, // ← Novo campo
    val imagemUrl: String? = null // Referencia para imagem remota
)