package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Producao
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface ProducaoRepository : JpaRepository<Producao, UUID> {
    fun findByFilialIdAndDataProducaoBetweenOrderByDataProducaoDesc(
        filialId: UUID,
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Producao>

    fun findAllByFilialIdOrderByDataProducaoDesc(filialId: UUID): List<Producao>

    fun findByIdAndFilialId(id: UUID, filialId: UUID): Producao?
}
