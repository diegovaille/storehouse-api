package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.util.*

@Entity
class Filial(
    @Id
    var id: UUID = UUID.randomUUID(),

    var nome: String = "",
    var cnpj: String? = null,
    @Column(name = "razao_social")
    var razaoSocial: String? = null,
    var endereco: String? = null,
    @Column(name = "logo_url")
    var logoUrl: String? = null,
    var ativo: Boolean = true,

    @ManyToOne
    @JoinColumn(name = "organizacao_id")
    var organizacao: Organizacao
)