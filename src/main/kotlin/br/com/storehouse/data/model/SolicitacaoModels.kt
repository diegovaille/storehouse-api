package br.com.storehouse.data.model

import br.com.storehouse.data.enums.StatusSolicitacao

data class SolicitacaoRequest(
    val descricaoItem: String,
    val categoria: String? = null,
    val nomeSolicitante: String,
    val contato: String,
    val observacao: String? = null
)

data class SolicitacaoUpdateRequest(
    val status: StatusSolicitacao? = null,
    val notificar: Boolean = false
)

data class SolicitacaoResponse(
    val id: String,
    val descricaoItem: String,
    val categoria: String?,
    val nomeSolicitante: String,
    val contato: String,
    val observacao: String?,
    val status: StatusSolicitacao,
    val dataCriacao: String,
    val dataAtualizacao: String?,
    val notificadoEm: String?
)
