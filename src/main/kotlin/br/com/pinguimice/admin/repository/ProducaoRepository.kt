package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Producao
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface ProducaoRepository : JpaRepository<Producao, UUID> {
    fun findByDataProducaoBetweenOrderByDataProducaoDesc(
        inicio: LocalDateTime,
        fim: LocalDateTime
    ): List<Producao>
    
    fun findAllByOrderByDataProducaoDesc(): List<Producao>
}
