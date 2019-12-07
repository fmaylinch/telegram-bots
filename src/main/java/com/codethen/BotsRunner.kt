package com.codethen

import com.codethen.telegram.RegisterBots
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Takes values from application.properties and register bots.
 */
@Component
class BotsRunner(
        @Value("\${mongo.url}") val connectionString: String,
        @Value("\${mongo.database}") val databaseName: String,
        @Value("\${telegram.lanxat.token}") val lanxatToken: String
    ) : CommandLineRunner {

    override fun run(vararg args: String?) {

        RegisterBots.registerBots(connectionString, databaseName, lanxatToken)
    }
}