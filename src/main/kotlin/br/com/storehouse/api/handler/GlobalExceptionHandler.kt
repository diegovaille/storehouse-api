package br.com.storehouse.api.handler

import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntidadeNaoEncontradaException::class)
    fun handleNotFound(ex: EntidadeNaoEncontradaException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.message)

    /**
     * Negação de @PreAuthorize é 403, não 500.
     *
     * Sem este handler, o @ExceptionHandler(Exception) abaixo engole o AccessDeniedException
     * e devolve "Erro inesperado: Access Denied" com status 500 — dizendo ao cliente que o
     * servidor quebrou, quando na verdade ele não tem permissão. Só apareceu agora porque o
     * SolicitacaoInternaController é o primeiro com um @PreAuthorize que de fato NEGA: o do
     * AdminUsuarioController usava hasAuthority('ADMIN'), que nunca casava com o ROLE_ADMIN
     * concedido pelo filtro, então nunca chegava a lançar.
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado: perfil sem permissão para esta operação")

    @ExceptionHandler(RequisicaoInvalidaException::class)
    fun handleBadRequest(ex: RequisicaoInvalidaException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.message)

    @ExceptionHandler(EstadoInvalidoException::class)
    fun handleConflict(ex: EstadoInvalidoException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ex.message)

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            "Erro inesperado: ${ex.message}"
        )
}
