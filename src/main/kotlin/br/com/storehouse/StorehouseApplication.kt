package br.com.storehouse

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class StorehouseApplication

fun main(args: Array<String>) {
    runApplication<StorehouseApplication>(*args)
}