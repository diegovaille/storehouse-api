package br.com.pinguimice.admin.model

import java.time.LocalDateTime
import java.util.*

data class ProducaoRequest(
    val saborId: UUID,
    val quantidadeProduzida: Int,
    val deduzirEstoque: Boolean = true,
    val dataProducao: LocalDateTime? = null,
    val observacoes: String? = null
)

data class ProducaoResponse(
    val id: UUID,
    val saborId: UUID,
    val saborNome: String,
    val quantidadeProduzida: Int,
    val deduzirEstoque: Boolean,
    val dataProducao: LocalDateTime,
    val observacoes: String?
)
