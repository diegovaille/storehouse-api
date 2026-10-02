package br.com.storehouse.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import software.amazon.awssdk.services.s3.S3Client

class AwsStorageClientTest {
    private val s3 = mock(S3Client::class.java)

    @Test
    fun `public origin uses the object key and never exposes the S3 API endpoint`() {
        val client = AwsStorageClient(s3, "storehouse-images", "https://account.r2.cloudflarestorage.com",
            "auto", " https://imagens.primeira.app.br/ ")
        assertEquals("https://imagens.primeira.app.br/produtos/filial/capa%20a%2Bb.jpg",
            client.getUrl("/produtos/filial/capa a+b.jpg"))
    }

    @Test
    fun `existing endpoint URLs remain unchanged without public origin`() {
        val client = AwsStorageClient(s3, "images", "https://storage.example/", "us-east-1")
        assertEquals("https://storage.example/images/path/photo.jpg", client.getUrl("path/photo.jpg"))
    }

    @Test
    fun `AWS regional URLs remain available without custom endpoint`() {
        val client = AwsStorageClient(s3, "images", region = "sa-east-1")
        assertEquals("https://images.s3.sa-east-1.amazonaws.com/photo.jpg", client.getUrl("photo.jpg"))
    }
}
