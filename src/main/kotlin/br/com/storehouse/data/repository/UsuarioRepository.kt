package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UsuarioRepository : JpaRepository<Usuario, UUID> {
    fun findByEmail(email: String): Usuario?
    fun findByUsername(username: String): Usuario?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}

