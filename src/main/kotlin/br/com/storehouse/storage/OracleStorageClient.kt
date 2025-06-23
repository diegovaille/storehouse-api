package br.com.storehouse.storage

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import java.io.ByteArrayInputStream

class OracleStorageClient(
    private val objectStorage: ObjectStorageClient,
    private val namespace: String,
    private val bucketName: String,
    private val region: String // e.g., "sa-saopaulo-1"
) : StorageClient {

    override fun upload(key: String, content: ByteArray, contentType: String) {
        val request = PutObjectRequest.builder()
            .bucketName(bucketName)
            .namespaceName(namespace)
            .objectName(key)
            .putObjectBody(ByteArrayInputStream(content))
            .contentLength(content.size.toLong())
            .contentType(contentType)
            .build()

        objectStorage.putObject(request)
    }

    override fun getUrl(key: String): String =
        "https://${namespace}.objectstorage.${region}.oci.customer-oci.com/n/$namespace/b/$bucketName/o/$key"
}
