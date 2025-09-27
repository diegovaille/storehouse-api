package br.com.storehouse.service

import br.com.storehouse.data.model.VendaResponse
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

@Service
class RelatorioService {
    fun gerarPdf(vendas: List<VendaResponse>): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = PdfWriter(baos)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        document.add(Paragraph("Relatório de Vendas").setFontSize(18f))
        document.add(
            Paragraph("Período: ${vendas.minOfOrNull { it.data }} - ${vendas.maxOfOrNull { it.data }}")
        )
        document.add(Paragraph("\n"))

        // 🔹 Agrupamento por produto
        val vendasAgrupamentoPorProduto = vendas
            .flatMap { it.itens }
            .groupBy { it.produtoNome }
            .mapValues { (_, itens) ->
                val qtdTotal = itens.sumOf { it.quantidade }
                val totalVenda = itens.sumOf { it.precoUnitario.multiply(it.quantidade.toBigDecimal()) }
                val totalCusto = itens.sumOf { it.precoCusto!!.multiply(it.quantidade.toBigDecimal()) }
                val estoqueAtual = itens.firstOrNull()?.estoque ?: 0

                ResumoProduto(qtdTotal, totalVenda, totalCusto, estoqueAtual)
            }

        val table = Table(floatArrayOf(5f, 2f, 3f, 3f, 3f, 2f))
        table.addHeaderCell("Item")
        table.addHeaderCell("Qtd Vendida")
        table.addHeaderCell("Total Vendas")
        table.addHeaderCell("Custo Total")
        table.addHeaderCell("Lucro")
        table.addHeaderCell("Estoque Atual")

        var somaVendas = BigDecimal.ZERO
        var somaCustos = BigDecimal.ZERO

        vendasAgrupamentoPorProduto.forEach { (produtoNome, resumo) ->
            val lucro = resumo.totalVenda.subtract(resumo.totalCusto)

            somaVendas = somaVendas.add(resumo.totalVenda)
            somaCustos = somaCustos.add(resumo.totalCusto)

            table.addCell(produtoNome)
            table.addCell(resumo.qtdVendida.toString())
            table.addCell("R$ ${resumo.totalVenda.setScale(2)}")
            table.addCell("R$ ${resumo.totalCusto.setScale(2)}")
            table.addCell("R$ ${lucro.setScale(2)}")
            table.addCell(resumo.estoqueAtual.toString())
        }

        document.add(table)
        document.add(Paragraph("\n"))

        val lucroTotal = somaVendas.subtract(somaCustos)


        val boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)

        document.add(Paragraph("Resumo Financeiro:").setFont(boldFont).setFontSize(18f))
        document.add(Paragraph("Total Vendido: R$ ${somaVendas.setScale(2)}"))
        document.add(Paragraph("Custo Total: R$ ${somaCustos.setScale(2)}"))
        document.add(Paragraph("Lucro: R$ ${lucroTotal.setScale(2)}"))

        document.close()

        return baos.toByteArray()
    }
}

data class ResumoProduto(
    val qtdVendida: Int,
    val totalVenda: BigDecimal,
    val totalCusto: BigDecimal,
    val estoqueAtual: Int
)
