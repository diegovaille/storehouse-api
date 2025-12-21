package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.MateriaPrima
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MateriaPrimaRepository : JpaRepository<MateriaPrima, UUID> {
    fun findByFilialIdOrderByDataCriacaoDesc(filialId: UUID): List<MateriaPrima>
    fun findByIdAndFilialId(id: UUID, filialId: UUID): MateriaPrima?

    fun findByFilialIdAndSaborIdOrderByDataCriacaoAsc(filialId: UUID, saborId: UUID): List<MateriaPrima>
    fun findByFilialIdAndSaborIdIsNull(filialId: UUID): List<MateriaPrima>
}
