package com.codethen.telegram.lanxatbot.exception;

import com.codethen.telegram.lanxatbot.profile.UserProfile;

public class ProfileNotConfiguredException extends LanXatException {

    private final UserProfile userProfile;

    public ProfileNotConfiguredException(UserProfile userProfile) {
        super("Profile not configured for userId " + userProfile.getId());
        this.userProfile = userProfile;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }
}
