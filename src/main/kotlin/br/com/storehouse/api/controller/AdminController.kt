package br.com.storehouse.api.controller

import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.service.UsuarioService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
    @RequestMapping("/api/admin")
class AdminController(private val usuarioService: UsuarioService) {

    @PostMapping("/usuarios")
    fun cadastrarUsuario(@RequestBody payload: Map<String, String>): Usuario {
        val email = payload["email"] ?: throw IllegalArgumentException("Email obrigatório")
        val role = payload["role"] ?: throw IllegalArgumentException("Role obrigatória")
        return usuarioService.cadastrar(email, role)
    }
}
