package com.codethen.telegram.lanxatbot.profile;

import javax.annotation.Nullable;

public interface UserProfileRepository {

    @Nullable UserProfile getProfileById(Integer userId);
    void saveOrUpdate(UserProfile profile);
}
