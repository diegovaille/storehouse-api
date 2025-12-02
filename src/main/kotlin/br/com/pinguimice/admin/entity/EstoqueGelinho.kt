package br.com.pinguimice.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "estoque_gelinho",
    schema = "pinguim",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_estoque_gelinho_sabor",
            columnNames = ["sabor_id"]
        )
    ]
)
class EstoqueGelinho(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sabor_id", nullable = false)
    var sabor: Sabor,

    @Column(nullable = false)
    var quantidade: Int,

    @Column(name = "ultima_atualizacao", nullable = false)
    var ultimaAtualizacao: LocalDateTime = LocalDateTime.now()
)
