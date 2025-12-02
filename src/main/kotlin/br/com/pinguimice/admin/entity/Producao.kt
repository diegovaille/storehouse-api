package br.com.pinguimice.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "producao", schema = "pinguim")
class Producao(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sabor_id", nullable = false)
    var sabor: Sabor,

    @Column(name = "quantidade_produzida", nullable = false)
    var quantidadeProduzida: Int,

    @Column(name = "deduzir_estoque", nullable = false)
    var deduzirEstoque: Boolean = true,

    @Column(name = "data_producao", nullable = false)
    var dataProducao: LocalDateTime = LocalDateTime.now(),

    @Column(columnDefinition = "TEXT")
    var observacoes: String? = null
)
