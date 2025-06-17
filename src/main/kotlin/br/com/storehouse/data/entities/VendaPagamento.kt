package br.com.storehouse.data.entities

import br.com.storehouse.data.enums.TipoPagamento
import jakarta.persistence.*
import java.util.*

@Entity
class VendaPagamento(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne
    @JoinColumn(name = "venda_id")
    var venda: Venda,

    @Enumerated(EnumType.STRING)
    var tipo: TipoPagamento = TipoPagamento.PIX, // DINHEIRO, CARTAO, PIX, etc.

    @Column(nullable = false, precision = 10, scale = 2)
    var valor: Double = 0.0
)
