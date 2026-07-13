package br.com.storehouse.data.entities

import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "solicitacao_interna")
class SolicitacaoInterna(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    var produto: Produto? = null,

    @Column(name = "descricao_item", nullable = false, length = 255)
    var descricaoItem: String,

    @Column(nullable = false)
    var quantidade: Int,

    @Column(name = "solicitante_email", nullable = false, length = 120)
    var solicitanteEmail: String,

    @Column(length = 500)
    var observacao: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusSolicitacaoInterna = StatusSolicitacaoInterna.SOLICITADO,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now(),

    @Column(name = "data_atualizacao")
    var dataAtualizacao: LocalDateTime? = null,

    @Column(name = "data_recebimento")
    var dataRecebimento: LocalDateTime? = null
)
