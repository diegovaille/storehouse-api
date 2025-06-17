package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Venda
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface VendaRepository : JpaRepository<Venda, UUID> {
    fun findByFilialIdAndDataBetweenOrderByDataDesc(filialId: UUID, inicio: LocalDateTime, fim: LocalDateTime): List<Venda>
}
