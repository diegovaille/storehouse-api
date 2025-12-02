package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.MateriaPrima
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MateriaPrimaRepository : JpaRepository<MateriaPrima, UUID> {
    fun findBySaborIdOrderByDataCriacaoAsc(saborId: UUID): List<MateriaPrima>
    fun findBySaborIdIsNull(): List<MateriaPrima>
}
