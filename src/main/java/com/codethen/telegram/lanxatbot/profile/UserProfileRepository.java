package com.codethen.telegram.lanxatbot.profile;

import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;

public interface UserProfileRepository {

    UserProfile getProfileById(Long userId) throws ProfileNotExistsException;
    void saveOrUpdate(UserProfile profile);
}
