package br.com.pinguimice.admin.entity

import br.com.storehouse.data.entities.Filial
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "outros", schema = "pinguim")
class Outros(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    var nome: String,

    @Column(name = "quantidade_entrada", nullable = false)
    var quantidadeEntrada: Int,

    @Column(name = "preco_entrada", nullable = false, precision = 10, scale = 4)
    var precoEntrada: BigDecimal,

    @Column(name = "unidades_por_item", nullable = false)
    var unidadesPorItem: Int,

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
