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
        val client = storageClientFactory.get(imgBucket)
        val extensao = nomeArquivo.substringAfterLast('.', "dat")
        val key = "despesas/$despesaId.$extensao"

        logger.info("Uploading expense attachment to: bucket=$imgBucket, key=$key")
        logger.info("File size: ${arquivo.size} bytes")
        logger.info("Content type: $contentType")

        client.upload(key = key, contentType = contentType, content = arquivo)

        return client.getUrl(key)
    }
}
