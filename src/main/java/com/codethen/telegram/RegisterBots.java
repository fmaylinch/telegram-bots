package com.codethen.telegram;

import com.codethen.ApiKeys;
import com.codethen.profile.MongoUserProfileRepository;
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

        final MongoUserProfileRepository mongoUserProfileRepository = new MongoUserProfileRepository();
        // fillUserProfiles(mongoProfileRepository);

        api.registerBot(
                new LanXatTelegramBot(
                    YandexServiceFactory.build(),
                        mongoUserProfileRepository));

        System.out.println("Bots registered!");
    }

    private static void fillUserProfiles(MongoUserProfileRepository mongoUserProfileRepository) {

        System.out.println("Filling some user profiles in mongo db");
        mongoUserProfileRepository.saveOrUpdate(USER_PROFILE_1);
        mongoUserProfileRepository.saveOrUpdate(USER_PROFILE_2);
    }

    private static UserProfileRepository buildFakeUserProfileRepo() {

        return new UserProfileRepository() {

            private Map<Integer, UserProfile> fakeUserProfiles = new HashMap<>();
            {
                fakeUserProfiles.put(USER_PROFILE_1.getUserId(), USER_PROFILE_1);
                fakeUserProfiles.put(USER_PROFILE_2.getUserId(), USER_PROFILE_2);
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

    public static final UserProfile USER_PROFILE_1 = new UserProfile(143015357,
            "ru", "en",
            "ru", "en",
            ApiKeys.YANDEX_API_KEY
    );
    public static final UserProfile USER_PROFILE_2 = new UserProfile(1065512701,
            "ru", "en",
            "en", "ru",
            ApiKeys.YANDEX_API_KEY
    );
}
