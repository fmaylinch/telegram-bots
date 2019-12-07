package com.codethen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TelegramBotsSpringBootApp

fun main(args: Array<String>) {
    runApplication<TelegramBotsSpringBootApp>(*args)
}
