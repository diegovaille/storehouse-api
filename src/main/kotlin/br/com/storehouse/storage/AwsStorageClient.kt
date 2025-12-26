package br.com.storehouse.storage

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AwsStorageClient(
    private val s3Client: S3Client,
    private val bucketName: String,
    private val endpoint: String? = null,
    private val region: String? = null
) : StorageClient {

    override fun upload(key: String, content: ByteArray, contentType: String) {
        val putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(putRequest, RequestBody.fromBytes(content))
    }

    override fun getUrl(key: String): String {
        val safeKey = key.trimStart('/')
        val encodedKey = safeKey.split('/').joinToString("/") {
            URLEncoder.encode(it, StandardCharsets.UTF_8).replace("+", "%20")
        }

        val ep = endpoint?.trim()?.trimEnd('/')
        return if (!ep.isNullOrBlank()) {
            "$ep/$bucketName/$encodedKey"
        } else {
            val r = region?.trim().orEmpty()
            if (r.isBlank() || r == "us-east-1") {
                "https://$bucketName.s3.amazonaws.com/$encodedKey"
            } else {
                "https://$bucketName.s3.$r.amazonaws.com/$encodedKey"
            }
        }
    }
}
