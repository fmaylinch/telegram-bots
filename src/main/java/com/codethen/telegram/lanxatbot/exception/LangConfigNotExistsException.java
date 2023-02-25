package com.codethen.telegram.lanxatbot.exception;

public class LangConfigNotExistsException extends LanXatException {

    private final String config;

    public LangConfigNotExistsException(String config) {
        super("Lang config " + config + " doesn't exist");
        this.config = config;
    }

    public String getConfig() {
        return config;
    }
}
