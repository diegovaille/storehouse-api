package br.com.storehouse.data.model

import br.com.storehouse.data.enums.StatusSolicitacaoInterna

data class SolicitacaoInternaRequest(
    val descricaoItem: String,
    val produtoId: String? = null,
    val quantidade: Int,
    val observacao: String? = null
)

data class SolicitacaoInternaUpdateRequest(
    val status: StatusSolicitacaoInterna? = null,
    val produtoId: String? = null
)

data class SolicitacaoInternaResponse(
    val id: String,
    val descricaoItem: String,
    val produtoId: String?,
    val produtoNome: String?,
    val quantidade: Int,
    val solicitanteEmail: String,
    val observacao: String?,
    val status: StatusSolicitacaoInterna,
    val dataCriacao: String,
    val dataAtualizacao: String?,
    val dataRecebimento: String?
)
