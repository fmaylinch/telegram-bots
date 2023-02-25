package com.codethen.telegram.lanxatbot.exception;

import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;

public class InlineQueryException extends RuntimeException {

    private final InlineQuery inlineQuery;

    public InlineQueryException(InlineQuery inlineQuery, String message) {
        super(message);
        this.inlineQuery = inlineQuery;
    }

    public InlineQueryException(InlineQuery inlineQuery, String message, Throwable cause) {
        super(message, cause);
        this.inlineQuery = inlineQuery;
    }

    public InlineQuery getQuery() {
        return inlineQuery;
    }
}
