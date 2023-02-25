package com.codethen.telegram.lanxatbot.exception;

import com.codethen.telegram.lanxatbot.profile.UserProfile;

public class ProfileNotEnabledException extends LanXatException {

    private final UserProfile userProfile;

    public ProfileNotEnabledException(UserProfile userProfile) {
        super("Profile not not enabled for userId " + userProfile.getId());
        this.userProfile = userProfile;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }
}
