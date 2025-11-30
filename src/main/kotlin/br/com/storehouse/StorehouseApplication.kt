package br.com.storehouse

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone


@SpringBootApplication
class StorehouseApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    runApplication<StorehouseApplication>(*args)
}