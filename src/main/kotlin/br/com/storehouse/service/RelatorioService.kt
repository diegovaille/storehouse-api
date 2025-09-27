package br.com.storehouse.service

import br.com.storehouse.data.model.VendaResponse
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Service
class RelatorioService {
    fun gerarPdf(vendas: List<VendaResponse>): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = PdfWriter(baos)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val dataInicio = vendas.minOfOrNull { it.data }?.format(formatter) ?: "-"
        val dataFim = vendas.maxOfOrNull { it.data }?.format(formatter) ?: "-"

        document.add(Paragraph("Relatório de Vendas").setFontSize(18f))
        document.add(Paragraph("Período: $dataInicio  -  $dataFim"))
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

        val table = Table(floatArrayOf(5f, 1f, 3f, 3f, 3f, 2f))
        table.setWidth(UnitValue.createPercentValue(100f)) // ocupa 100% da largura da página

        table.addHeaderCell("Item")
        table.addHeaderCell("Qtd Vendida")
        table.addHeaderCell("Total Vendas")
        table.addHeaderCell("Custo Total")
        table.addHeaderCell("Lucro")
        table.addHeaderCell("Estoque Atual")

        var totalVendas = BigDecimal.ZERO
        var totalCustos = BigDecimal.ZERO

        vendasAgrupamentoPorProduto.forEach { (produtoNome, resumo) ->
            val lucro = resumo.totalVenda.subtract(resumo.totalCusto)

            totalVendas = totalVendas.add(resumo.totalVenda)
            totalCustos = totalCustos.add(resumo.totalCusto)

            table.addCell(produtoNome)
            table.addCell(Cell().add(Paragraph(resumo.qtdVendida.toString())).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell().add(Paragraph("R$ ${resumo.totalVenda.setScale(2)}")).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell().add(Paragraph("R$ ${resumo.totalCusto.setScale(2)}")).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell().add(Paragraph("R$ ${lucro.setScale(2)}")).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(resumo.estoqueAtual.toString())
        }

        document.add(table)
        document.add(Paragraph("\n"))

        val lucroTotal = totalVendas.subtract(totalCustos)

        val boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)

        document.add(Paragraph("\nResumo Financeiro:").setFont(boldFont).setFontSize(14f))
        val resumo = Table(floatArrayOf(3f, 3f, 3f))

        resumo.setWidth(UnitValue.createPercentValue(100f))
        resumo.addHeaderCell("Total Vendas")
        resumo.addHeaderCell("Total Custos")
        resumo.addHeaderCell("Lucro Total")

        resumo.addCell(Cell().add(Paragraph("R$ ${totalVendas.setScale(2)}")).setTextAlignment(TextAlignment.RIGHT))
        resumo.addCell(Cell().add(Paragraph("R$ ${totalCustos.setScale(2)}")).setTextAlignment(TextAlignment.RIGHT))
        resumo.addCell(Cell().add(Paragraph("R$ ${lucroTotal.setScale(2)}")).setTextAlignment(TextAlignment.RIGHT))

        document.add(resumo)
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
