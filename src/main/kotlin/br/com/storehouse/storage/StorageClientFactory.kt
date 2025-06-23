package br.com.storehouse.storage

import com.oracle.bmc.Region
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.auth.SimplePrivateKeySupplier
import com.oracle.bmc.objectstorage.ObjectStorageClient
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Component
class StorageClientFactory(private val props: ProjectBucketProperties) {

    private val logger = LoggerFactory.getLogger(StorageClientFactory::class.java)
    private val clients = mutableMapOf<String, StorageClient>()

    @PostConstruct
    fun init() {
        props.buckets.forEach { bucket ->
            val client = when (bucket.provider) {
                "aws" -> buildAwsClient(bucket)
                "oracle" -> buildOracleClient(bucket)
                else -> throw IllegalArgumentException("Provider inválido: ${bucket.provider}")
            }

            clients[bucket.name] = client
        }
    }

    fun get(bucket: String): StorageClient =
        clients[bucket]
            ?: throw IllegalArgumentException("StorageClient não encontrado para bucket: $bucket")

    fun getDefault(): StorageClient = get(props.defaultBucket)

    private fun buildAwsClient(bucket: ProjectBucketProperties.BucketConfig): StorageClient {
        val client = S3Client.builder()
            .region(software.amazon.awssdk.regions.Region.of(bucket.region))
            .endpointOverride(URI.create(bucket.endpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(bucket.accessKey, bucket.secretKey)
                )
            )
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
            .build()

        return AwsStorageClient(client, bucket.name)
    }

    private fun buildOracleClient(bucket: ProjectBucketProperties.BucketConfig): StorageClient {
        val provider = SimpleAuthenticationDetailsProvider.builder()
            .tenantId(bucket.tenantId!!)
            .userId(bucket.userId)
            .fingerprint(bucket.fingerprint!!)
            .privateKeySupplier(SimplePrivateKeySupplier(bucket.privateKey))
            .build()

        val objectStorageClient = ObjectStorageClient.builder()
            .region(Region.fromRegionId(bucket.region))
            .endpoint(bucket.endpoint)
            .build(provider)

        return OracleStorageClient(objectStorageClient, bucket.namespace!!, bucket.name, bucket.region)
    }
}