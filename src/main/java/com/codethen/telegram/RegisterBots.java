package com.codethen.telegram;

import com.codethen.profile.CachedUserProfileRepository;
import com.codethen.profile.MongoUserProfileRepository;
import com.codethen.telegram.lanxatbot.LanXatTelegramBot;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.yandex.YandexApiFactory;
import com.codethen.yandex.YandexService;
import com.mongodb.reactivestreams.client.MongoClients;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

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

        final UserProfileRepository mongoUserProfileRepository =
                new CachedUserProfileRepository(new MongoUserProfileRepository(
                        MongoClients.create(connectionString), databaseName));

        // fillUserProfiles(mongoUserProfileRepository);

        api.registerBot(
                new LanXatTelegramBot(
                        botName,
                        lanxatApiToken,
                        new YandexService(YandexApiFactory.build()),
                        mongoUserProfileRepository));

        System.out.println("Bots registered!");
    }

    private static String getEnvChecked(String name) {
        final String value = System.getenv(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing environment variable: " + name);
        }
        return value;
    }

    private static void fillUserProfiles(UserProfileRepository userProfileRepository) {

        System.out.println("Filling some user profiles in mongo db");
        // userProfileRepository.saveOrUpdate(someProfile1);
        // userProfileRepository.saveOrUpdate(someProfile2);
    }

    private static UserProfileRepository buildFakeUserProfileRepo() {

        return new UserProfileRepository() {

            private Map<Integer, UserProfile> fakeUserProfiles = new HashMap<>(); {
                //fakeUserProfiles.put(someUserId, someProfile1);
                //fakeUserProfiles.put(someUserId, someProfile2);
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
                fakeUserProfiles.put(profile.getId(), profile);
            }
        };
    }
}
