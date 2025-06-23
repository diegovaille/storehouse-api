package br.com.storehouse.storage

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class AwsStorageClient(
    private val s3Client: S3Client,
    private val bucketName: String
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
        return "https://$bucketName.s3.amazonaws.com/$key"
    }

}
