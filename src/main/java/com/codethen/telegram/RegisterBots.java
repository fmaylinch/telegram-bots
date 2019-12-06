package com.codethen.telegram;

import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.yandex.YandexServiceFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class RegisterBots {

    public static void main(String... args) throws TelegramApiRequestException {

        System.out.println("Registering bots...");

        ApiContextInitializer.init();
        final TelegramBotsApi api = new TelegramBotsApi();

        api.registerBot(new LanXatTelegramBot(YandexServiceFactory.build()));

        System.out.println("Bots registered!");
    }
}
