package br.com.storehouse.s3

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "project.s3")
class ProjectS3Properties {
    lateinit var defaultBucket: String
    var buckets: List<BucketConfig> = emptyList()

    class BucketConfig {
        lateinit var name: String
        lateinit var region: String
        lateinit var endpoint: String
        lateinit var accessKey: String
        lateinit var secretKey: String
        var namespace: String? = null
    }
}
