package br.com

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone


@SpringBootApplication(scanBasePackages = ["br.com.storehouse", "br.com.pinguimice"])
class Application

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    runApplication<Application>(*args)
}