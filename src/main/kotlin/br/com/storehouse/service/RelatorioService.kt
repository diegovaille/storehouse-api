package br.com.storehouse.service

import br.com.storehouse.data.model.VendaResponse
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

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
            .mapValues { entry ->
                val itens = entry.value
                val qtdTotal = itens.sumOf { it.quantidade }
                val totalVenda = itens.sumOf { it.precoUnitario.multiply(it.quantidade.toBigDecimal()) }
                // se já vem estoque no response, pode pegar do primeiro ou calcular
                val estoqueAtual = itens.firstOrNull()?.estoque ?: 0
                Triple(qtdTotal, totalVenda, estoqueAtual)
            }

        val table = Table(floatArrayOf(5f, 2f, 3f, 2f))
        table.addHeaderCell("Item")
        table.addHeaderCell("Qtd Vendida")
        table.addHeaderCell("Total Vendas")
        table.addHeaderCell("Estoque Atual")

        vendasAgrupamentoPorProduto.forEach { (produtoNome, triple) ->
            val (qtd, totalVenda, estoqueAtual) = triple

            table.addCell(produtoNome)
            table.addCell(qtd.toString())
            table.addCell("R$ ${totalVenda.setScale(2)}")
            table.addCell(estoqueAtual.toString())
        }

        document.add(table)
        document.close()

        return baos.toByteArray()
    }
}
