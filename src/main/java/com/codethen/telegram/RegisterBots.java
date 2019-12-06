package com.codethen.telegram;

import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.yandex.YandexService;
import com.codethen.yandex.YandexServiceFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class RegisterBots {

    public static void main(String... args) throws TelegramApiRequestException {

        System.out.println("Registering bots...");

        ApiContextInitializer.init();
        final TelegramBotsApi api = new TelegramBotsApi();

        final YandexService yandex = YandexServiceFactory.build();
        api.registerBot(new LanXatTelegramBot(yandex));

        System.out.println("Bots registered!");
    }
}
