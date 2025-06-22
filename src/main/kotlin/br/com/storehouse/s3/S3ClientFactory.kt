package br.com.storehouse.s3

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Component
class S3ClientFactory(
    private val props: ProjectS3Properties
) {

    private val clients = mutableMapOf<String, S3Client>()
    private val endpointMap = mutableMapOf<String, String>()
    private val namespaceMap = mutableMapOf<String, String>()

    @PostConstruct
    fun init() {
        props.buckets.forEach { bucket ->
            val uri = URI.create(bucket.endpoint)
            val isAws = bucket.endpoint.contains("amazonaws.com")

            val builder = S3Client.builder()
                .region(Region.of(bucket.region))
                .endpointOverride(uri)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(bucket.accessKey, bucket.secretKey)
                    )
                )

            if (!isAws) {
                builder.serviceConfiguration(
                    software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build()
                )
            }

            val client = builder.build()

            clients[bucket.name] = client
            endpointMap[bucket.name] = bucket.endpoint
            namespaceMap[bucket.name] = bucket.namespace ?: ""
        }
    }

    fun getUrl(bucket: String, key: String): String {
        val endpoint = endpointMap[bucket]
            ?: throw IllegalArgumentException("Endpoint não configurado para bucket: $bucket")

        val base = endpoint.removeSuffix("/")

        return when {
            base.contains("amazonaws.com") -> "https://$bucket.s3.amazonaws.com/$key"
            base.contains("localhost") || base.contains("127.0.0.1") -> {
                val publicUrl = "https://primeiraigrejastoretest.loca.lt"
                "$publicUrl/$bucket/$key"
            }
            else -> {
                val namespace = namespaceMap[bucket]
                    ?: throw IllegalArgumentException("Namespace não configurado para bucket: $bucket")
                "$base/n/$namespace/b/$bucket/o/$key"
            }
        }
    }

    fun getDefaultUrl(key: String): String = getUrl(props.defaultBucket, key)

    fun get(bucketName: String): S3Client =
        clients[bucketName]
            ?: throw IllegalArgumentException("S3Client não configurado para bucket: $bucketName")

    fun getDefault(): S3Client = get(props.defaultBucket)
}
