package br.com.pinguimice.admin.entity

import br.com.storehouse.data.entities.Filial
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "cliente", schema = "pinguim")
class Cliente(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 255)
    var nome: String,

    @Column(columnDefinition = "TEXT")
    var endereco: String? = null,

    @Column(length = 20)
    var telefone: String? = null,

    @Column(length = 18)
    var cnpj: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regiao_id")
    var regiao: RegiaoVenda? = null,

    @Column(nullable = false)
    var bloqueado: Boolean = false,

    @Column(name = "motivo_bloqueio", columnDefinition = "TEXT")
    var motivoBloqueio: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now()
)

