package br.com.storehouse.runner

import br.com.storehouse.service.StorageService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.util.*

/**
 * Runner local só para testar upload no Storage.
 *
 * IMPORTANTE: não deve rodar em produção.
 */
@Profile("dev")
@Component
class UploadTestRunner(
    private val service: StorageService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val resource = ClassPathResource("images/logo.png")
        if (!resource.exists()) {
            // Não quebra a aplicação caso o recurso não exista no ambiente local.
            return
        }

        val imagem = resource.inputStream.use { it.readBytes() }
        val url = service.uploadImagemProduto(UUID.randomUUID(), "789525790001", imagem)
        println("✅ Upload realizado com sucesso:")
        println("➡️ URL: $url")
    }
}
