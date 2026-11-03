package br.com.storehouse.service


import br.com.storehouse.storage.StorageClientFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.*

@Service
@Profile("!test")
class DefaultStorageService(private val storageClientFactory: StorageClientFactory) : StorageService {

    private val logger = LoggerFactory.getLogger(DefaultStorageService::class.java)

    @Value("\${project.storage.default-bucket}")
    private lateinit var imgBucket: String
    @Value("\${project.storage.despesa-bucket}")
    private lateinit var despesaBucket: String

    override fun uploadImagemProduto(filialId: UUID, codigoBarras: String, imagem: ByteArray): String {
        val client = storageClientFactory.get(imgBucket)

        val key = "produtos/$filialId/$codigoBarras.jpg"

        logger.info("Uploading image to: bucket=$imgBucket, key=$key")
        logger.info("Image size: ${imagem.size} bytes")
        logger.info("Image content type: image/jpeg")
        logger.info("Using S3 client: $client")

        client.upload(key = key, contentType = "image/jpeg", content = imagem)

        return client.getUrl(key)
    }

    override fun uploadAnexoDespesa(despesaId: UUID, nomeArquivo: String, arquivo: ByteArray, contentType: String): String {
        val client = storageClientFactory.get(despesaBucket)
        val extensao = nomeArquivo.substringAfterLast('.', "dat").lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,10}")) } ?: "dat"
        // Immutable file keys keep DB snapshots consistent when an attachment is replaced.
        val key = "despesas/$despesaId/${UUID.randomUUID()}.$extensao"

        logger.info("Uploading expense attachment to: bucket=$despesaBucket, key=$key")
        logger.info("File size: ${arquivo.size} bytes")
        logger.info("Content type: $contentType")

        client.upload(key = key, contentType = contentType, content = arquivo)

        return client.getUrl(key)
    }

    override fun downloadAnexoDespesa(despesaId: UUID, reference: String): br.com.storehouse.storage.StoredFile {
        // anexoUrl can be supplied in JSON: never trust a reference to another expense.
        val prefix = "local://$despesaBucket/despesas/$despesaId/"
        if (!reference.startsWith(prefix) || reference.removePrefix(prefix).contains('/')) {
            throw br.com.storehouse.exceptions.EntidadeNaoEncontradaException("Anexo não encontrado")
        }
        return storageClientFactory.get(despesaBucket).download(reference)
    }
}
