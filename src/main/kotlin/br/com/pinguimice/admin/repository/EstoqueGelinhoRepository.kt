package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.EstoqueGelinho
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EstoqueGelinhoRepository : JpaRepository<EstoqueGelinho, UUID> {
    fun findBySaborId(saborId: UUID): EstoqueGelinho?
}
