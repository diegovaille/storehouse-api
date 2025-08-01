package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
class VendaItem(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne
    @JoinColumn(name = "venda_id")
    var venda: Venda,

    @ManyToOne
    @JoinColumn(name = "produto_id")
    var produto: Produto,

    var quantidade: Int = 0,

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    var precoUnitario: BigDecimal
)
