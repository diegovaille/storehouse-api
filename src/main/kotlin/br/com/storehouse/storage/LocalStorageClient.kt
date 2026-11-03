package br.com.storehouse.storage

import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import org.springframework.http.MediaType
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Private storage: references are resolved only by the authenticated expense endpoint. */
class LocalStorageClient(directory: Path, bucket: String) : StorageClient {
    private val root = Files.createDirectories(directory.toAbsolutePath().normalize()).toRealPath()
    private val prefix = "local://$bucket/"

    private fun pathFor(key: String): Path {
        val parts = key.split('/')
        require(parts.isNotEmpty() && parts.all { it.matches(Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*")) }) {
            "Chave de arquivo inválida"
        }
        val target = root.resolve(key).normalize()
        require(target.startsWith(root)) { "Chave de arquivo inválida" }
        var path = root
        for (part in parts) {
            path = path.resolve(part)
            require(!Files.isSymbolicLink(path)) { "Link simbólico não permitido" }
        }
        return target
    }

    private fun replace(path: Path, content: ByteArray) {
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, ".attachment-", ".tmp")
        try {
            Files.write(temporary, content)
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override fun upload(key: String, content: ByteArray, contentType: String) {
        val mediaType = MediaType.parseMediaType(contentType).toString()
        require(!mediaType.contains('\r') && !mediaType.contains('\n'))
        replace(pathFor("$key.content-type"), mediaType.toByteArray(Charsets.UTF_8))
        replace(pathFor(key), content)
    }

    override fun getUrl(key: String): String {
        pathFor(key)
        return prefix + key
    }

    override fun download(reference: String): StoredFile {
        if (!reference.startsWith(prefix)) throw EntidadeNaoEncontradaException("Anexo não encontrado")
        val key = reference.removePrefix(prefix)
        val path = try { pathFor(key) } catch (_: IllegalArgumentException) {
            throw EntidadeNaoEncontradaException("Anexo não encontrado")
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw EntidadeNaoEncontradaException("Anexo não encontrado")
        }
        val metadata = pathFor("$key.content-type")
        val type = if (Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)) {
            Files.readString(metadata)
        } else "application/octet-stream"
        return StoredFile(Files.readAllBytes(path), type, path.fileName.toString())
    }
}
