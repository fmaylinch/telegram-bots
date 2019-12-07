package com.codethen.telegram.lanxatbot.profile;

public interface UserProfileRepository {
    UserProfile getProfile(String userName);
}
