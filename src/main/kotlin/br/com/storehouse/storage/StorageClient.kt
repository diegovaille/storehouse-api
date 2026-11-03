package br.com.storehouse.storage

interface StorageClient {
    fun upload(key: String, content: ByteArray, contentType: String)
    fun getUrl(key: String): String
    fun download(reference: String): StoredFile =
        throw br.com.storehouse.exceptions.EntidadeNaoEncontradaException("Anexo não encontrado")
}

data class StoredFile(val content: ByteArray, val contentType: String, val filename: String)
