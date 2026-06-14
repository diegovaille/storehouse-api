package br.com.storehouse.data.enums

enum class StatusSolicitacao {
    SOLICITADO,   // registrado; admin precisa adquirir
    SEPARADO,     // adquirido e separado -> aparece em Reservas
    RETIRADO,     // cliente retirou
    CANCELADO
}
