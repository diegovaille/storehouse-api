package br.com.storehouse.data.enums

enum class StatusSolicitacaoInterna {
    SOLICITADO,   // staff registrou o pedido de compra
    COMPRADO,     // compra efetuada, aguardando chegar
    RECEBIDO,     // chegou -> estoque foi somado (terminal)
    CANCELADO     // terminal
}
