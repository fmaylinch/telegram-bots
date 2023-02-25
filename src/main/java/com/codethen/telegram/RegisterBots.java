package com.codethen.telegram;

import com.codethen.google.GoogleTranslateService;
import com.codethen.profile.CachedUserProfileRepository;
import com.codethen.profile.MongoUserProfileRepository;
import com.codethen.search.MongoSearchRepository;
import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.telegram.lanxatbot.search.SearchRepository;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class RegisterBots {

    public static void main(String[] args) throws TelegramApiException {

        // TODO: Setup logger

        System.out.println("Reading environment variables...");

        final String connectionString = getEnvChecked("mongodb.connectionString");
        final String databaseName = getEnvChecked("mongodb.databaseName");
        final String telegramBotName = getEnvChecked("telegram.bots.api.lanxat.name");
        final String telegramBotApiToken = getEnvChecked("telegram.bots.api.lanxat.token");

        registerBots(connectionString, databaseName, telegramBotName, telegramBotApiToken);
    }

    public static void registerBots(String connectionString, String databaseName, String botName, String lanxatApiToken) throws TelegramApiException {

        System.out.println("Registering bots...");

        final TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);

        final MongoClient mongoClient = MongoClients.create(connectionString);

        final UserProfileRepository userProfileRepository =
                new CachedUserProfileRepository(new MongoUserProfileRepository(
                    mongoClient, databaseName));

        final SearchRepository searchRepository =
            new MongoSearchRepository(mongoClient, databaseName);

        var session = api.registerBot(
                new LanXatTelegramBot(
                        botName,
                        lanxatApiToken,
                        //new YandexTranslateService(YandexApiFactory.build()),
                        new GoogleTranslateService(),
                        userProfileRepository,
                        searchRepository));

        System.out.println("Bot registered: " + botName);
    }

    private static String getEnvChecked(String name) {
        final String value = System.getenv(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing environment variable: " + name);
        }
        return value;
    }
}
