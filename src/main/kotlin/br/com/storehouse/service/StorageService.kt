package br.com.storehouse.service

import java.util.*

interface StorageService {
    fun uploadImagemProduto(filialId: UUID, codigoBarras: String, imagem: ByteArray): String
    fun uploadAnexoDespesa(despesaId: UUID, nomeArquivo: String, arquivo: ByteArray, contentType: String): String
    fun downloadAnexoDespesa(despesaId: UUID, reference: String): br.com.storehouse.storage.StoredFile =
        throw br.com.storehouse.exceptions.EntidadeNaoEncontradaException("Anexo não encontrado")
}
