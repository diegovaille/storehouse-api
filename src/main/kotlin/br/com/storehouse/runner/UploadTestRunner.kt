package br.com.storehouse.runner

import br.com.storehouse.service.DefaultStorageService
import br.com.storehouse.service.StorageService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Paths
import java.util.*

@Profile("!prod", "!test")
@Component
class UploadTestRunner(
    private val service: StorageService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val imagePath = Paths.get("src/main/resources/images/logo.png").toFile()
        val imagem = imagePath.readBytes()
        val url = service.uploadImagemProduto(UUID.randomUUID(), "789525790001", imagem)
        println("✅ Upload realizado com sucesso:")
        println("➡️ URL: $url")
    }
}
