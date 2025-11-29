package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Perfil
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PerfilRepository : JpaRepository<Perfil, UUID> {
    fun findByTipo(tipo: String): Perfil?
}