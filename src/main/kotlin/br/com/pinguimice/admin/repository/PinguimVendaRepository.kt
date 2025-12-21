package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.PinguimVenda
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface PinguimVendaRepository : JpaRepository<PinguimVenda, UUID> {
    fun findByFilialIdAndDataVendaBetweenOrderByDataVendaDesc(
        filialId: UUID,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<PinguimVenda>

    fun findByIdAndFilialId(id: UUID, filialId: UUID): PinguimVenda?
}
