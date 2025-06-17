package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
class Produto(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var codigoBarras: String = "",

    @Column(nullable = false)
    var nome: String,

    @Column(nullable = false)
    var excluido: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_id", nullable = false)
    var tipo: br.com.storehouse.data.entities.TipoProduto,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "imagem_url")
    var imagemUrl: String? = null,

    @Column(nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now(),

    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "estado_atual_id")
    var estadoAtual: ProdutoEstado? = null,

    @OneToOne(mappedBy = "produto", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var descricao: br.com.storehouse.data.entities.ProdutoDescricao? = null
)