package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.RegiaoVenda
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RegiaoVendaRepository : JpaRepository<RegiaoVenda, UUID> {
    fun findByAtivoTrue(): List<RegiaoVenda>
}
