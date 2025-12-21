package br.com.pinguimice.admin.model

import java.time.LocalDateTime

data class ParametroCalculoRequest(
    val chave: String,
    val valor: Double,
    val descricao: String? = null
)

data class ParametroCalculoResponse(
    val chave: String,
    val valor: Double,
    val descricao: String?,
    val dataAtualizacao: LocalDateTime
)

