package com.codethen.telegram;

import com.codethen.ApiKeys;
import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.yandex.YandexServiceFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

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

    private static final UserProfile FAKE_USER_PROFILE_FERRAN =
            new UserProfile(143015357,
                    ApiKeys.YANDEX_API_KEY,
                    "ru", "en",
                    "ru", "en"
            );

    private static Map<Integer, UserProfile> fakeUserProfiles = new HashMap<>();
    static {

        fakeUserProfiles.put(143015357,
                new UserProfile(143015357,
                ApiKeys.YANDEX_API_KEY,
                "ru", "en",
                "ru", "en"
        ));

        fakeUserProfiles.put(1065512701,
                new UserProfile(1065512701,
                ApiKeys.YANDEX_API_KEY,
                "ru", "en",
                "en", "ru"
        ));
    }

    private static UserProfileRepository buildFakeUserProfileRepo() {

        return new UserProfileRepository() {

            @Nullable
            public UserProfile getProfile(Integer userId) {

                if (fakeUserProfiles.containsKey(userId)) {
                    System.out.println("Returning fake profile for userId: " + userId);
                    return fakeUserProfiles.get(userId);
                } else {
                    System.out.println("No fake user profile for userId: " + userId);
                    return null;
                }
            }
        };
    }
}
