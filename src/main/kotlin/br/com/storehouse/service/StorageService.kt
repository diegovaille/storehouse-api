package br.com.storehouse.service

import java.util.*

interface StorageService {
    fun uploadImagemProduto(filialId: UUID, codigoBarras: String, imagem: ByteArray): String
}
