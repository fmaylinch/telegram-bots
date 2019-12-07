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

    private static UserProfileRepository buildFakeUserProfileRepo() {

        return new UserProfileRepository() {

            private Map<Integer, UserProfile> fakeUserProfiles = new HashMap<>();
            {
                fakeUserProfiles.put(143015357,
                        new UserProfile(143015357,
                                "ru", "en",
                                "ru", "en",
                                ApiKeys.YANDEX_API_KEY
                        ));

                fakeUserProfiles.put(1065512701,
                        new UserProfile(1065512701,
                                "ru", "en",
                                "en", "ru",
                                ApiKeys.YANDEX_API_KEY
                        ));
            }

            @Nullable
            public UserProfile getProfileById(Integer userId) {

                if (fakeUserProfiles.containsKey(userId)) {
                    final UserProfile profile = fakeUserProfiles.get(userId);
                    // System.out.println("Returning fake profile: " + profile);
                    return profile;
                } else {
                    System.out.println("No fake user profile for userId: " + userId);
                    return null;
                }
            }

            @Override
            public void saveOrUpdate(UserProfile profile) {
                System.out.println("Updating profile: " + profile);
                fakeUserProfiles.put(profile.getUserId(), profile);
            }
        };
    }
}
