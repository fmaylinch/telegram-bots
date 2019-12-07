package com.codethen.telegram;

import com.codethen.ApiKeys;
import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.yandex.YandexServiceFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class RegisterBots {

    public static void main(String... args) throws TelegramApiRequestException {

        System.out.println("Registering bots...");

        ApiContextInitializer.init();
        final TelegramBotsApi api = new TelegramBotsApi();

        api.registerBot(
                new LanXatTelegramBot(
                    YandexServiceFactory.build(),
                    buildFakeUserProfileRepo())); // TODO: Use a real repository

        System.out.println("Bots registered!");
    }

    private static final UserProfile FAKE_USER_PROFILE =
            new UserProfile(ApiKeys.YANDEX_API_KEY, "ru", "en");

    private static UserProfileRepository buildFakeUserProfileRepo() {

        return new UserProfileRepository() {
            public UserProfile getProfile(String userName) {
                System.out.println("Returning fake profile for: " + userName);
                return FAKE_USER_PROFILE;
            }
        };
    }
}
