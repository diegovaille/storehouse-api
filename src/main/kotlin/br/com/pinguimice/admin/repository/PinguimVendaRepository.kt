package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.PinguimVenda
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

interface PinguimVendaRepository : JpaRepository<PinguimVenda, UUID> {
    fun findByDataVendaBetweenOrderByDataVendaDesc(
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<PinguimVenda>
}
