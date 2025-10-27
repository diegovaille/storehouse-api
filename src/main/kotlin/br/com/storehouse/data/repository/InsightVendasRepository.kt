package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.InsightVendas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface InsightVendasRepository : JpaRepository<InsightVendas, UUID> {
    fun findByFilialIdAndDataInicioAndDataFim(filialId: UUID, dataInicio: LocalDate, dataFim: LocalDate): InsightVendas?
}
