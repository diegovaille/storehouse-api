package br.com.storehouse.service

import br.com.storehouse.data.model.ItemVendaResponse
import br.com.storehouse.data.model.VendaResponse
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.UUID

class RelatorioServiceTest {

    @Test
    fun `gerarPdf deve refletir valorTotal da venda e nao aplicar desconto novamente`() {
        val service = RelatorioService()

        // Simula venda com voucher já aplicado no backend:
        // - precoUnitario já veio pela metade (ex 12.50)
        // - valorTotal já é final e não deve ser reduzido novamente
        val venda = VendaResponse(
            id = UUID.randomUUID(),
            valorTotal = BigDecimal("25.00"),
            data = "2025-12-30T13:52:07",
            vendedorNome = "diego",
            vendedorEmail = "diego@x.com",
            cancelada = false,
            itens = listOf(
                ItemVendaResponse(
                    produtoNome = "Livro A",
                    categoria = "Livro",
                    quantidade = 2,
                    precoUnitario = BigDecimal("12.50"), // poderia induzir duplicação se recalculasse
                    estoque = 10,
                    precoCusto = BigDecimal("5.00")
                )
            )
        )

        val pdfBytes = service.gerarPdf(listOf(venda))

        val text = PdfDocument(PdfReader(ByteArrayInputStream(pdfBytes))).use { pdf ->
            buildString {
                for (i in 1..pdf.numberOfPages) {
                    append(PdfTextExtractor.getTextFromPage(pdf.getPage(i)))
                    append('\n')
                }
            }
        }

        // O relatório imprime "R$ {totalVendas}". Dependendo da locale, pode aparecer com vírgula.
        assertTrue(
            text.contains("R$ 25,00") || text.contains("R$ 25.00"),
            "Texto extraído do PDF deveria conter o total de vendas 25.00 (valorTotal persistido). Texto: $text"
        )
    }
}
