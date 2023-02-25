package com.codethen;

import com.codethen.telegram.RegisterBots;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BotsRunner implements CommandLineRunner {

    private final String connectionString;
    private final String databaseName;
    private final String lanxatToken;
    private final String lanxatName;

    public BotsRunner(
            @Value("${mongo.url}") String connectionString,
            @Value("${mongo.database}") String databaseName,
            @Value("${telegram.lanxat.token}") String lanxatToken,
            @Value("${telegram.lanxat.name}") String lanxatName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        this.lanxatToken = lanxatToken;
        this.lanxatName = lanxatName;
    }

    @Override
    public void run(String... args) throws Exception {
        RegisterBots.registerBots(connectionString, databaseName, lanxatName, lanxatToken);
    }
}
