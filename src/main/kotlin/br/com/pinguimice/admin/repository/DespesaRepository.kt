package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Despesa
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface DespesaRepository : JpaRepository<Despesa, UUID> {
    fun findByFilialIdAndDataCriacaoBetweenOrderByDataCriacaoDesc(
        filialId: UUID,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Despesa>

    fun findAllByFilialIdOrderByDataCriacaoDesc(filialId: UUID): List<Despesa>

    fun findByIdAndFilialId(id: UUID, filialId: UUID): Despesa?
}
