package br.com.pinguimice.admin.entity

import br.com.storehouse.data.entities.Filial
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "embalagem", schema = "pinguim")
class Embalagem(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    var nome: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sabor_id")
    var sabor: Sabor? = null,

    @Column(name = "quantidade_kg", nullable = false, precision = 10, scale = 4)
    var quantidadeKg: BigDecimal,

    @Column(name = "preco_kg", nullable = false, precision = 10, scale = 4)
    var precoKg: BigDecimal,

    @Column(name = "total_unidades", nullable = false)
    var totalUnidades: Int,

    @Column(name = "preco_por_unidade", nullable = false, precision = 10, scale = 4)
    var precoPorUnidade: BigDecimal,

    @Column(name = "estoque_unidades", nullable = false)
    var estoqueUnidades: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now()
)
