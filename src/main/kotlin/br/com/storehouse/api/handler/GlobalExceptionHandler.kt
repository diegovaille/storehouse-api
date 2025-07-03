package br.com.storehouse.api.handler

import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntidadeNaoEncontradaException::class)
    fun handleNotFound(ex: EntidadeNaoEncontradaException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.message)

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
