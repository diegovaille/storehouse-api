package br.com.pinguimice.admin.entity

import br.com.storehouse.data.entities.Filial
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "despesa", schema = "pinguim")
class Despesa(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 255)
    var descricao: String,

    @Column(nullable = false, precision = 10, scale = 2)
    var valor: BigDecimal,

    @Column(name = "data_vencimento")
    var dataVencimento: LocalDate? = null,

    @Column(name = "data_pagamento")
    var dataPagamento: LocalDate? = null,

    @Column(name = "anexo_url", length = 255)
    var anexoUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    var observacao: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now()
)
