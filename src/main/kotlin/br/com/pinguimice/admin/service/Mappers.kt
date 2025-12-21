package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.*
import br.com.pinguimice.admin.model.*

fun MateriaPrima.toResponse() = MateriaPrimaResponse(
    id = this.id,
    nome = this.nome,
    saborId = this.sabor?.id,
    saborNome = this.sabor?.nome,
    tipoEntrada = TipoEntrada.valueOf(this.tipoEntrada),
    quantidadeEntrada = this.quantidadeEntrada,
    precoEntrada = this.precoEntrada,
    totalUnidades = this.totalUnidades,
    precoPorUnidade = this.precoPorUnidade,
    estoqueUnidades = this.estoqueUnidades,
    dataCriacao = this.dataCriacao
)

fun Embalagem.toResponse() = EmbalagemResponse(
    id = this.id,
    nome = this.nome,
    saborId = this.sabor?.id,
    saborNome = this.sabor?.nome,
    quantidadeKg = this.quantidadeKg,
    precoKg = this.precoKg,
    totalUnidades = this.totalUnidades,
    precoPorUnidade = this.precoPorUnidade,
    estoqueUnidades = this.estoqueUnidades,
    dataCriacao = this.dataCriacao
)

fun Outros.toResponse() = OutrosResponse(
    id = this.id,
    nome = this.nome,
    quantidadeEntrada = this.quantidadeEntrada,
    precoEntrada = this.precoEntrada,
    unidadesPorItem = this.unidadesPorItem,
    totalUnidades = this.totalUnidades,
    precoPorUnidade = this.precoPorUnidade,
    estoqueUnidades = this.estoqueUnidades,
    dataCriacao = this.dataCriacao
)

fun Sabor.toResponse() = SaborResponse(
    id = this.id,
    nome = this.nome,
    corHex = this.corHex,
    ativo = this.ativo,
    dataCriacao = this.dataCriacao
)

fun Producao.toResponse() = ProducaoResponse(
    id = this.id,
    saborId = this.sabor.id,
    saborNome = this.sabor.nome,
    quantidadeProduzida = this.quantidadeProduzida,
    deduzirEstoque = this.deduzirEstoque,
    dataProducao = this.dataProducao,
    observacoes = this.observacoes
)

fun EstoqueGelinho.toResponse() = EstoqueGelinhoResponse(
    id = this.id,
    saborId = this.sabor.id,
    saborNome = this.sabor.nome,
    quantidade = this.quantidade,
    ultimaAtualizacao = this.ultimaAtualizacao
)

fun RegiaoVenda.toResponse() = RegiaoVendaResponse(
    id = this.id,
    nome = this.nome,
    descricao = this.descricao,
    ativo = this.ativo,
    dataCriacao = this.dataCriacao
)

fun Despesa.toResponse() = DespesaResponse(
    id = this.id,
    descricao = this.descricao,
    valor = this.valor,
    dataVencimento = this.dataVencimento?.toString(),
    dataPagamento = this.dataPagamento?.toString(),
    anexoUrl = this.anexoUrl,
    observacao = this.observacao,
    dataCriacao = this.dataCriacao
)
