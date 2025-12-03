package br.com.pinguimice.admin.entity

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

    @Column(length = 255)
    var cliente: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regiao_id", nullable = false)
    var regiao: RegiaoVenda,

    @Column(name = "usuario_id", nullable = false)
    var usuarioId: UUID,

    @OneToMany(mappedBy = "venda", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itens: List<PinguimVendaItem> = mutableListOf()
)
