package br.com.storehouse.data.entities

import jakarta.persistence.*
import java.util.*

@Entity
class Organizacao(
    @Id
    var id: UUID = UUID.randomUUID(),

    var nome: String = "",
    var cnpj: String? = null,
    @Column(name = "razao_social")
    var razaoSocial: String? = null,
    var endereco: String? = null,
    var municipio: String = "",
    var estado: String = "", // UF
    var tipo: String = "", // loja, armazem, etc.
    @Column(name = "logo_url")
    var logoUrl: String? = null,

    @OneToMany(mappedBy = "organizacao", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var filiais: List<Filial> = mutableListOf()
)