package br.com.storehouse.data.model

enum class TipoUsuario(val value: String) {
    ADMIN("Administrador"),
    VENDEDOR("Vendedor");

    companion object {
        fun from(descricao: String): TipoUsuario {
            return values().find { it.value.equals(descricao, ignoreCase = true) }
                ?: throw IllegalArgumentException("Tipo de usuário inválido: $descricao")
        }

        fun listar(): List<String> {
            return values().map { it.value }
        }
    }
}