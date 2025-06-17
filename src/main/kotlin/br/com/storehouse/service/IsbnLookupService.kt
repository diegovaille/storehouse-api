package br.com.storehouse.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class IsbnLookupService(
    private val webClient: WebClient,
    @Value("\${isbndb.api-key}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(IsbnLookupService::class.java)

    fun buscarInformacoesPorIsbn(isbn: String): Map<String, Any> {
        logger.info("Buscando informações no ISBNdb para ISBN: {}", isbn)

        val url = "https://api.isbndb.com/book/$isbn"

        val response = webClient.get()
            .uri(url)
            .header("Authorization", apiKey)
            .retrieve()
            .bodyToMono(Map::class.java)
            .doOnError { logger.error("Erro ao buscar ISBN {}: {}", isbn, it.message) }
            .block()

        logger.info("Resposta da API ISBNdb: {}", response)

        val book = (response?.get("book") as? Map<*, *>)

        if (book == null) {
            logger.warn("Livro não encontrado para ISBN {}", isbn)
            return emptyMap()
        }

        return mapOf(
            "titulo" to (book["title"] ?: ""),
            "autor" to ((book["authors"] as? List<*>)?.joinToString(", ") ?: ""),
            "editora" to (book["publisher"] ?: ""),
            "imagem" to (book["image"] ?: ""),
            "imagem_original" to (book["image_original"] ?: ""),
            "ano" to (book["date_published"]?.toString()?.take(4) ?: "")
        )
    }
}