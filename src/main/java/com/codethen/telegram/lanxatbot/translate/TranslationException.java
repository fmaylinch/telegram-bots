package com.codethen.telegram.lanxatbot.translate;

public class TranslationException extends RuntimeException {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
