package br.com.storehouse.data.entities

import br.com.storehouse.data.enums.StatusSolicitacao
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "solicitacao")
class Solicitacao(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "descricao_item", nullable = false, length = 255)
    var descricaoItem: String,

    @Column(length = 60)
    var categoria: String? = null,

    @Column(name = "nome_solicitante", nullable = false, length = 120)
    var nomeSolicitante: String,

    @Column(nullable = false, length = 40)
    var contato: String,

    @Column(length = 500)
    var observacao: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusSolicitacao = StatusSolicitacao.SOLICITADO,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now(),

    @Column(name = "data_atualizacao")
    var dataAtualizacao: LocalDateTime? = null,

    @Column(name = "notificado_em")
    var notificadoEm: LocalDateTime? = null
)
