package br.com.pinguimice.admin.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigDecimal

/**
 * Aceita BigDecimal vindo como:
 * - number JSON (10.5)
 * - string com ponto ("10.5")
 * - string com vírgula pt-BR ("10,5")
 * - string com separador de milhar ("1.234,56" ou "1,234.56")
 */
class LenientBigDecimalDeserializer : JsonDeserializer<BigDecimal>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BigDecimal {
        val node: JsonNode = p.codec.readTree(p)

        val raw: String = when {
            node.isNumber -> node.decimalValue().toPlainString()
            node.isTextual -> node.asText()
            else -> ctxt.handleUnexpectedToken(BigDecimal::class.java, p) as String
        }

        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return BigDecimal.ZERO

        // remove espaços
        var normalized = trimmed.replace(" ", "")

        // Se contém vírgula e ponto, decidir separador decimal pelo último que aparecer
        val lastComma = normalized.lastIndexOf(',')
        val lastDot = normalized.lastIndexOf('.')

        normalized = if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                // decimal = ',', remove '.' milhares, troca ',' por '.'
                normalized.replace(".", "").replace(',', '.')
            } else {
                // decimal = '.', remove ',' milhares
                normalized.replace(",", "")
            }
        } else if (lastComma >= 0) {
            // apenas ',', tratar como decimal
            normalized.replace(".", "").replace(',', '.')
        } else {
            // apenas '.' ou nenhum
            normalized
        }

        return try {
            BigDecimal(normalized)
        } catch (_: Exception) {
            throw ctxt.weirdStringException(trimmed, BigDecimal::class.java, "Valor decimal inválido")
        }
    }
}
