package br.com.storehouse.data

import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.model.UsuarioAutenticado
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
data class SharedTestData(
    var usuarioAutenticado: UsuarioAutenticado? = null,
    var response: ResponseEntity<*>? = null,
    var filial: Filial? = null
) {
    fun clear() {
        usuarioAutenticado = null
        response = null
    }
}