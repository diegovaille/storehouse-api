package br.com.storehouse.service

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.util.UUID

@Service
@Profile("test")
class TestStorageService : StorageService {

    private val logger = LoggerFactory.getLogger(TestStorageService::class.java)
    private val testStorageDir = System.getProperty("java.io.tmpdir") + "/test-storage"

    init {
        // Create test storage directory
        File(testStorageDir).mkdirs()
        logger.info("Test storage initialized at: $testStorageDir")
    }

    override fun uploadImagemProduto(filialId: UUID, codigoBarras: String, imagem: ByteArray): String {
        val dir = File("$testStorageDir/produtos/$filialId")
        dir.mkdirs()

        val file = File(dir, "$codigoBarras.jpg")
        Files.write(file.toPath(), imagem)

        logger.info("Uploaded product image to: ${file.absolutePath}")
        return "file://${file.absolutePath}"
    }

    override fun uploadAnexoDespesa(despesaId: UUID, nomeArquivo: String, arquivo: ByteArray, contentType: String): String {
        val dir = File("$testStorageDir/despesas")
        dir.mkdirs()

        val extensao = nomeArquivo.substringAfterLast('.', "dat")
        val file = File(dir, "$despesaId.$extensao")
        Files.write(file.toPath(), arquivo)

        logger.info("Uploaded expense attachment to: ${file.absolutePath}")
        return "file://${file.absolutePath}"
    }
}