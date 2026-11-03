package br.com.storehouse.storage

import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LocalStorageClientTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `private attachment roundtrip keeps bytes and content type`() {
        val client = LocalStorageClient(directory, "despesas-files")
        val key = "despesas/expense/file.pdf"
        val bytes = "%PDF-1.4 test".toByteArray()
        client.upload(key, bytes, "application/pdf")
        val reference = client.getUrl(key)
        assertEquals("local://despesas-files/$key", reference)
        val file = client.download(reference)
        assertArrayEquals(bytes, file.content)
        assertEquals("application/pdf", file.contentType)
        assertEquals("file.pdf", file.filename)
    }

    @Test
    fun `traversal absolute paths encoded paths and foreign buckets are rejected`() {
        val client = LocalStorageClient(directory, "despesas-files")
        for (key in listOf("../secret", "/etc/passwd", "despesas/../secret", "despesas/%2e%2e/secret", "despesas/./file")) {
            assertThrows(IllegalArgumentException::class.java) { client.upload(key, byteArrayOf(1), "application/pdf") }
        }
        assertThrows(EntidadeNaoEncontradaException::class.java) { client.download("local://another/despesas/file.pdf") }
        assertThrows(EntidadeNaoEncontradaException::class.java) { client.download("local://despesas-files/../secret") }
        assertThrows(EntidadeNaoEncontradaException::class.java) { client.download("https://example.com/secret") }
    }

    @Test
    fun `symlink does not expose files outside private directory`() {
        val outside = Files.createTempDirectory("attachment-outside")
        try {
            Files.writeString(outside.resolve("secret.pdf"), "secret")
            Files.createSymbolicLink(directory.resolve("despesas"), outside)
            val client = LocalStorageClient(directory, "despesas-files")
            assertThrows(EntidadeNaoEncontradaException::class.java) {
                client.download("local://despesas-files/despesas/secret.pdf")
            }
        } finally {
            Files.deleteIfExists(outside.resolve("secret.pdf"))
            Files.deleteIfExists(outside)
        }
    }
}
