package br.com.storehouse.service

import br.com.storehouse.s3.S3ClientFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.*

@Service
class S3StorageService(private val s3ClientFactory: S3ClientFactory) : br.com.storehouse.service.StorageService {

    private val logger = LoggerFactory.getLogger(S3StorageService::class.java)

    @Value("\${project.s3.default-bucket}")
    private lateinit var imgBucket: String

    override fun uploadImagemProduto(filialId: UUID, codigoBarras: String, imagem: ByteArray): String {
        val client = s3ClientFactory.get(imgBucket)

        val key = "produtos/$filialId/$codigoBarras.jpg"

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(imgBucket)
            .key(key)
            .contentType("image/jpeg")
            .build()

        logger.info("Uploading image to S3: bucket=$imgBucket, key=$key")
        logger.info("Image size: ${imagem.size} bytes")
        logger.info("Image content type: image/jpeg")
        logger.info("Image upload request: $putObjectRequest")
        logger.info("Using S3 client: ${client.serviceName()}")

        client.putObject(putObjectRequest, RequestBody.fromBytes(imagem))

        return s3ClientFactory.getUrl(imgBucket, key)
    }
}
