package com.codethen.telegram.lanxatbot.exception;

public class LanXatException extends RuntimeException {

    public LanXatException(String message) {
        super(message);
    }

    public LanXatException(String message, Throwable cause) {
        super(message, cause);
    }
}
