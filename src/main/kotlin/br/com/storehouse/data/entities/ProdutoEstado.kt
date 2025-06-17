package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
class ProdutoEstado(
    @Id
    var id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    var produto: Produto,

    @Column(nullable = false)
    var estoque: Int = 0,

    @Column(nullable = false, precision = 10, scale = 2)
    var preco: Double = 0.0,

    @Column(name = "preco_custo", nullable = false, precision = 10, scale = 2)
    var precoCusto: Double = 0.0,

    @Column(name = "data_inicio", nullable = false)
    var dataInicio: LocalDateTime = LocalDateTime.now(),

    @Column(name = "data_fim")
    var dataFim: LocalDateTime? = null
)
