package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
class Venda(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var data: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    var vendedor: Usuario,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    var valorTotal: BigDecimal,

    @Column(nullable = false)
    var cancelada: Boolean = false,

    @Column(length = 20)
    var cliente: String? = null,

    @OneToMany(mappedBy = "venda", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itens: List<VendaItem> = mutableListOf(),

    @OneToMany(mappedBy = "venda", cascade = [CascadeType.ALL], orphanRemoval = true)
    var pagamentos: List<VendaPagamento> = mutableListOf()
)
