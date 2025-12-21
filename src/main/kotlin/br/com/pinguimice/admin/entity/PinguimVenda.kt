package br.com.pinguimice.admin.entity

import br.com.storehouse.data.entities.Filial
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "venda", schema = "pinguim")
class PinguimVenda(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, precision = 10, scale = 2)
    var total: BigDecimal,

    @Column(name = "total_pago", nullable = false, precision = 10, scale = 2)
    var totalPago: BigDecimal,

    @Column(name = "data_venda", nullable = false)
    var dataVenda: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    var cliente: Cliente? = null,

    @Column(name = "usuario_id", nullable = false)
    var usuarioId: UUID,

    @Column(name = "abater_estoque", nullable = false)
    var abaterEstoque: Boolean = true,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @OneToMany(mappedBy = "venda", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itens: MutableList<PinguimVendaItem> = mutableListOf()
)
