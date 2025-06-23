package br.com.storehouse

import br.com.storehouse.service.DefaultStorageService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File
import java.util.*

@Component
class UploadTestRunner(
    private val service: DefaultStorageService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val imagem = File("/Users/diegovaille/Git/pib/storehouse-api/src/main/resources/images/logo.png").readBytes()
        val url = service.uploadImagemProduto(UUID.randomUUID(), "789525790001", imagem)
        println("✅ Upload realizado com sucesso:")
        println("➡️ URL: $url")
    }
}
