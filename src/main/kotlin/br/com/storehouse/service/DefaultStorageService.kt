package br.com.storehouse.service


import br.com.storehouse.storage.StorageClientFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
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
}
