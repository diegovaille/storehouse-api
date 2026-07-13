package br.com.storehouse.service

import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.repository.ProdutoEstadoRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Dono único da cadeia temporal de ProdutoEstado.
 *
 * O estoque (e o preço, e o preço de custo) não vivem em Produto — vivem numa cadeia de
 * ProdutoEstado, onde o estado aberto (dataFim == null) é o atual. Mudar qualquer um dos três
 * significa FECHAR o estado atual e ABRIR um novo. Nunca mutar em lugar.
 *
 * Antes deste service, essa regra era reimplementada à mão em seis lugares — e um deles
 * (cancelamento de venda) fazia errado. Agora existe um lugar só, e ele trava a linha do
 * produto (SELECT ... FOR UPDATE) para que escritores concorrentes serializem.
 *
 * NINGUÉM constrói ProdutoEstado fora daqui. Há um comando de checagem no skill
 * estoque-temporal que falha se alguém tentar.
 */
@Service
class ProdutoEstadoService(
    private val produtoRepository: ProdutoRepository,
    private val produtoEstadoRepository: ProdutoEstadoRepository
) {

    /**
     * Primeiro estado de um produto recém-criado. Sem lock: ninguém mais conhece esse produto ainda.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun criarInicial(
        produto: Produto,
        estoque: Int,
        preco: BigDecimal,
        precoCusto: BigDecimal
    ): ProdutoEstado {
        if (estoque < 0) {
            throw EstadoInvalidoException("Estoque não pode ser negativo: $estoque")
        }
        val estado = ProdutoEstado(
            produto = produto,
            estoque = estoque,
            preco = preco,
            precoCusto = precoCusto
        )
        produtoEstadoRepository.save(estado)
        produto.estadoAtual = estado
        produtoRepository.save(produto)
        return estado
    }

    /**
     * Soma um delta ao estoque, preservando os preços. Venda: delta negativo.
     * Recebimento e cancelamento de venda: delta positivo.
     *
     * A checagem de estoque insuficiente acontece AQUI, sob o lock — é o que torna
     * impossível (e não apenas improvável) vender a descoberto em duas vendas simultâneas.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun aplicarDelta(produtoId: UUID, delta: Int): ProdutoEstado {
        val produto = travar(produtoId)
        val atual = estadoAtualDe(produto)

        // Delta zero não muda nada: não abre estado novo idêntico só para registrar um no-op.
        if (delta == 0) {
            return atual
        }

        val novoEstoque = atual.estoque + delta
        if (novoEstoque < 0) {
            throw EstadoInvalidoException(
                "Estoque insuficiente para o produto ${produto.codigoBarras}: " +
                    "atual ${atual.estoque}, delta $delta"
            )
        }

        return transicionar(produto, atual, novoEstoque, atual.preco, atual.precoCusto)
    }

    /**
     * Define valores absolutos. preco/precoCusto nulos = preservar os atuais.
     * Usado pela edição de produto e pelo PATCH de estoque.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun definir(
        produtoId: UUID,
        estoque: Int,
        preco: BigDecimal? = null,
        precoCusto: BigDecimal? = null
    ): ProdutoEstado {
        if (estoque < 0) {
            throw EstadoInvalidoException("Estoque não pode ser negativo: $estoque")
        }
        val produto = travar(produtoId)
        val atual = produto.estadoAtual

        // Produto legado/órfão sem estadoAtual (a coluna é nullable no banco): não há estado
        // para preservar nem para fechar, então cria o primeiro em vez de falhar — mesmo
        // comportamento self-healing que o antigo precisaNovoEstado tinha antes da
        // centralização. preco/precoCusto ausentes (PATCH de só-estoque) viram zero; quem
        // chamou fica responsável por completar os valores reais depois (tela de edição).
        if (atual == null) {
            return criarInicial(produto, estoque, preco ?: BigDecimal.ZERO, precoCusto ?: BigDecimal.ZERO)
        }

        val novoPreco = preco ?: atual.preco
        val novoPrecoCusto = precoCusto ?: atual.precoCusto

        // Nada mudou: não abre estado novo (evita poluir a cadeia com estados idênticos).
        if (atual.estoque == estoque && atual.preco == novoPreco && atual.precoCusto == novoPrecoCusto) {
            return atual
        }

        return transicionar(produto, atual, estoque, novoPreco, novoPrecoCusto)
    }

    private fun travar(produtoId: UUID): Produto =
        produtoRepository.findByIdForUpdate(produtoId)
            ?: throw EntidadeNaoEncontradaException("Produto $produtoId não encontrado")

    private fun estadoAtualDe(produto: Produto): ProdutoEstado =
        produto.estadoAtual
            ?: throw EstadoInvalidoException("Produto ${produto.id} não possui estado atual definido")

    /** Fecha o estado atual e abre um novo. O único lugar que faz isso. */
    private fun transicionar(
        produto: Produto,
        atual: ProdutoEstado,
        estoque: Int,
        preco: BigDecimal,
        precoCusto: BigDecimal
    ): ProdutoEstado {
        val agora = LocalDateTime.now()
        atual.dataFim = agora
        // saveAndFlush (não save): o ActionQueue do Hibernate ordena todos os INSERTs antes de
        // todos os UPDATEs no flush, então um save() comum aqui deixaria o INSERT do estado
        // novo (data_fim = NULL) chegar ao banco ANTES do UPDATE que fecha o estado atual —
        // as duas linhas ficariam com data_fim NULL ao mesmo tempo. Inofensivo hoje, mas rejeita
        // toda venda/cancelamento/edição assim que existir o índice único parcial
        // uk_produto_estado_aberto (produto_id) WHERE data_fim IS NULL, que não pode ser
        // DEFERRABLE por ser parcial. Forçar o flush aqui fecha o estado atual NO BANCO antes
        // de abrir o novo, então nunca há duas linhas abertas simultâneas para o mesmo produto.
        produtoEstadoRepository.saveAndFlush(atual)

        val novo = ProdutoEstado(
            produto = produto,
            estoque = estoque,
            preco = preco,
            precoCusto = precoCusto,
            dataInicio = agora
        )
        produtoEstadoRepository.save(novo)

        produto.estadoAtual = novo
        produtoRepository.save(produto)
        return novo
    }
}
