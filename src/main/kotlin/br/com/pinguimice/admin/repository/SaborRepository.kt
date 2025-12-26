package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Sabor
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SaborRepository : JpaRepository<Sabor, UUID> {
    fun findByAtivoTrue(): List<Sabor>
}
