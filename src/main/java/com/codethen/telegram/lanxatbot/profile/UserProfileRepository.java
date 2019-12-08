package com.codethen.telegram.lanxatbot.profile;

import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;

import javax.annotation.Nullable;

public interface UserProfileRepository {

    @Nullable UserProfile getProfileById(Integer userId) throws ProfileNotExistsException;
    void saveOrUpdate(UserProfile profile);
}
