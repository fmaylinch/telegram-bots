package com.codethen.telegram;

import com.codethen.profile.CachedUserProfileRepository;
import com.codethen.profile.MongoUserProfileRepository;
import com.codethen.search.MongoSearchRepository;
import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.telegram.lanxatbot.search.SearchRepository;
import com.codethen.yandex.YandexApiFactory;
import com.codethen.yandex.YandexService;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class RegisterBots {

    public static void main(String[] args) throws TelegramApiRequestException {

        // TODO: Setup logger

        System.out.println("Reading environment variables...");

        final String connectionString = getEnvChecked("mongodb.connectionString");
        final String databaseName = getEnvChecked("mongodb.databaseName");
        final String telegramBotName = getEnvChecked("telegram.bots.api.lanxat.name");
        final String telegramBotApiToken = getEnvChecked("telegram.bots.api.lanxat.token");

        registerBots(connectionString, databaseName, telegramBotName, telegramBotApiToken);
    }

    public static void registerBots(String connectionString, String databaseName, String botName, String lanxatApiToken) throws TelegramApiRequestException {

        System.out.println("Registering bots...");

        ApiContextInitializer.init();
        final TelegramBotsApi api = new TelegramBotsApi();

        final MongoClient mongoClient = MongoClients.create(connectionString);

        final UserProfileRepository userProfileRepository =
                new CachedUserProfileRepository(new MongoUserProfileRepository(
                    mongoClient, databaseName));

        final SearchRepository searchRepository =
            new MongoSearchRepository(mongoClient, databaseName);

        api.registerBot(
                new LanXatTelegramBot(
                        botName,
                        lanxatApiToken,
                        new YandexService(YandexApiFactory.build()),
                        userProfileRepository,
                        searchRepository));

        System.out.println("Bots registered!");
    }

    private static String getEnvChecked(String name) {
        final String value = System.getenv(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing environment variable: " + name);
        }
        return value;
    }
}
