package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "insight_vendas")
data class InsightVendas(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val filialId: UUID,
    val dataInicio: LocalDate,
    val dataFim: LocalDate,

    @Column(columnDefinition = "TEXT")
    val insight: String,

    val criadoEm: LocalDate = LocalDate.now()
)
