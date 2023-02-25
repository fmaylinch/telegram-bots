package com.codethen.telegram.lanxatbot.exception;

public class ProfileNotExistsException extends LanXatException {

    private final Long userId;

    public ProfileNotExistsException(Long userId) {
        super("Profile doesn't exist for userId " + userId);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
