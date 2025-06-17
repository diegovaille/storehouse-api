package br.com.storehouse.data.enums

enum class TipoProdutoEnum {
    LIVRO,
    CAMISETA,
    QUADRO,
    CANECA,
    BONE,
    CHAVEIRO,
    OUTROS;

    companion object {
        fun from(nome: String): TipoProdutoEnum? =
            entries.firstOrNull { it.name.equals(nome, ignoreCase = true) }
    }
}