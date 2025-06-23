package br.com.storehouse.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "project.storage")
class ProjectBucketProperties {
    lateinit var defaultBucket: String
    var buckets: List<BucketConfig> = emptyList()

    class BucketConfig {
        lateinit var name: String
        lateinit var region: String
        lateinit var provider: String
        var endpoint: String? = null
        var secretKey: String? = null
        var accessKey: String? = null
        var userId: String? = null
        var namespace: String? = null
        var tenantId: String? = null
        var fingerprint: String? = null
        var privateKey: String? = null
    }
}
