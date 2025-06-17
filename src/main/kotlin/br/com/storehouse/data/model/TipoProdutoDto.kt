package br.com.storehouse.data.model

import java.util.*

data class TipoProdutoDto(
    val id: UUID,
    val nome: String,
    val campos: Map<String, String>
)