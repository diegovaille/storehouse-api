package br.com.pinguimice.admin.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "venda_item", schema = "pinguim")
class PinguimVendaItem(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    var venda: PinguimVenda,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sabor_id", nullable = false)
    var sabor: Sabor,

    @Column(nullable = false)
    var quantidade: Int
)
