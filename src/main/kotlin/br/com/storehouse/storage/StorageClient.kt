package br.com.storehouse.storage

interface StorageClient {
    fun upload(key: String, content: ByteArray, contentType: String)
    fun getUrl(key: String): String
}