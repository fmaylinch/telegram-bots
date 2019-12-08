package com.codethen.telegram.lanxatbot.profile;

import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;

public interface UserProfileRepository {

    UserProfile getProfileById(Integer userId) throws ProfileNotExistsException;
    void saveOrUpdate(UserProfile profile);
}
